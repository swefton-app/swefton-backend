[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateNotNullOrEmpty()]
    [string]$Name
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$basePackage = "com.swefton.backend"
$projectRoot = Split-Path -Parent $PSScriptRoot
$packagePath = $basePackage.Replace('.', [IO.Path]::DirectorySeparatorChar)

if (-not (Test-Path -LiteralPath (Join-Path $projectRoot "pom.xml") -PathType Leaf)) {
    throw "Could not find pom.xml. Keep this script inside the project's scripts directory."
}

$trimmedName = $Name.Trim()
if ($trimmedName -notmatch '^[A-Za-z][A-Za-z0-9]*(?:[-_\s]+[A-Za-z0-9]+)*$') {
    throw "Invalid module name '$Name'. Use letters, numbers, spaces, hyphens, or underscores."
}

$nameParts = @($trimmedName -split '[-_\s]+' | Where-Object { $_.Length -gt 0 })
$className = ($nameParts | ForEach-Object {
    $_.Substring(0, 1).ToUpperInvariant() + $_.Substring(1).ToLowerInvariant()
}) -join ""
$moduleName = ($nameParts -join "").ToLowerInvariant()
$routeName = ($nameParts -join "-").ToLowerInvariant()
$tableName = ($nameParts -join "_").ToLowerInvariant()
$modulePackage = "$basePackage.modules.$moduleName"
$basePath = Join-Path $projectRoot "src\main\java\$packagePath\modules\$moduleName"

if (Test-Path -LiteralPath $basePath) {
    throw "Module '$moduleName' already exists at '$basePath'."
}

function Write-JavaSource {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$Content
    )

    $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
    [IO.File]::WriteAllText($Path, ($Content.TrimStart() + [Environment]::NewLine), $utf8WithoutBom)
}

$folders = @("api", "controller", "dto", "entity", "enums", "repository", "service")
foreach ($folder in $folders) {
    New-Item -ItemType Directory -Path (Join-Path $basePath $folder) -Force | Out-Null
}

Write-JavaSource -Path "$basePath\api\${className}Api.java" -Content @"
package $modulePackage.api;

public final class ${className}Api {

    public static final String BASE_PATH = "/api/v1/$routeName";

    private ${className}Api() {
    }
}
"@

Write-JavaSource -Path "$basePath\entity\${className}.java" -Content @"
package $modulePackage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "$tableName")
@Getter
@Setter
public class $className {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public $className() {
    }
}
"@

Write-JavaSource -Path "$basePath\dto\${className}Request.java" -Content @"
package $modulePackage.dto;

public record ${className}Request() {
}
"@

Write-JavaSource -Path "$basePath\dto\${className}Response.java" -Content @"
package $modulePackage.dto;

public record ${className}Response(Long id) {
}
"@

Write-JavaSource -Path "$basePath\repository\${className}Repository.java" -Content @"
package $modulePackage.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import $modulePackage.entity.$className;

public interface ${className}Repository extends JpaRepository<$className, Long> {
}
"@

Write-JavaSource -Path "$basePath\service\${className}Service.java" -Content @"
package $modulePackage.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import $modulePackage.dto.${className}Request;
import $modulePackage.dto.${className}Response;
import $modulePackage.entity.$className;
import $modulePackage.repository.${className}Repository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ${className}Service {

    private final ${className}Repository repository;

    @Transactional(readOnly = true)
    public List<${className}Response> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ${className}Response findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public ${className}Response create(${className}Request request) {
        $className entity = new $className();
        return toResponse(repository.save(entity));
    }

    @Transactional
    public ${className}Response update(Long id, ${className}Request request) {
        $className entity = findEntityById(id);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findEntityById(id));
    }

    private $className findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "$className not found"));
    }

    private ${className}Response toResponse($className entity) {
        return new ${className}Response(entity.getId());
    }
}
"@

Write-JavaSource -Path "$basePath\controller\${className}Controller.java" -Content @"
package $modulePackage.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import $modulePackage.api.${className}Api;
import $modulePackage.dto.${className}Request;
import $modulePackage.dto.${className}Response;
import $modulePackage.service.${className}Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(${className}Api.BASE_PATH)
public class ${className}Controller {

    private final ${className}Service service;

    @GetMapping
    public ResponseEntity<List<${className}Response>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<${className}Response> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<${className}Response> create(
            @Valid @RequestBody ${className}Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<${className}Response> update(
            @PathVariable Long id,
            @Valid @RequestBody ${className}Request request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
"@

Write-Host ""
Write-Host "Module '$className' created successfully."
Write-Host "Location: $basePath"
Write-Host "Package:  $modulePackage"
Write-Host "Endpoint: /api/v1/$routeName"
Write-Host ""
Write-Host "Generated files:"
Write-Host "  api/${className}Api.java"
Write-Host "  controller/${className}Controller.java"
Write-Host "  dto/${className}Request.java"
Write-Host "  dto/${className}Response.java"
Write-Host "  entity/${className}.java"
Write-Host "  repository/${className}Repository.java"
Write-Host "  service/${className}Service.java"
