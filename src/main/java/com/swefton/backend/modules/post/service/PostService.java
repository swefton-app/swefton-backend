package com.swefton.backend.modules.post.service;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.image.repository.ImageRepository;
import com.swefton.backend.modules.image.enums.ImageType;
import com.swefton.backend.modules.post.dto.*;
import com.swefton.backend.modules.post.entity.*;
import com.swefton.backend.modules.post.repository.*;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.entity.UserProfile;
import com.swefton.backend.modules.user.repository.UserProfileRepository;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.modules.video.entity.Video;
import com.swefton.backend.modules.video.repository.VideoRepository;
import com.swefton.backend.session.ISessionUser;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class PostService {
    private final PostRepository posts; private final PostLikeRepository likes; private final PostCommentRepository comments;
    private final UserRepository users; private final UserProfileRepository profiles; private final FacilityRepository facilities;
    private final ImageRepository images; private final VideoRepository videos; private final ISessionUser session;

    @Transactional(readOnly = true)
    public PostPageResponse feed(Long facilityId, String authorType, boolean mine, LocalDateTime cursorAt, Long cursorId, int size) {
        if ((cursorAt == null) != (cursorId == null)) throw bad("Both cursorCreatedAt and cursorId are required");
        String filter = authorType == null || authorType.isBlank() ? null : authorType.trim().toUpperCase(Locale.ROOT);
        if (filter != null && !Set.of("TRAINER", "FACILITY").contains(filter)) throw bad("Author filter must be TRAINER or FACILITY");
        int limit = Math.min(Math.max(size, 1), 50);
        Specification<Post> criteria = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (mine) predicates.add(cb.equal(root.get("author").get("id"), session.getUserId()));
            if (facilityId != null) predicates.add(cb.equal(root.get("facility").get("id"), facilityId));
            if ("TRAINER".equals(filter)) predicates.add(cb.isNull(root.get("facility")));
            if ("FACILITY".equals(filter)) predicates.add(cb.isNotNull(root.get("facility")));
            if (cursorAt != null) predicates.add(cb.or(
                    cb.lessThan(root.get("createdAt"), cursorAt),
                    cb.and(cb.equal(root.get("createdAt"), cursorAt), cb.lessThan(root.get("id"), cursorId))));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        List<Post> found = posts.findAll(criteria, PageRequest.of(0, limit + 1,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))).getContent();
        boolean more = found.size() > limit;
        List<Post> page = more ? found.subList(0, limit) : found;
        Set<Long> liked = page.isEmpty() ? Set.of() : new HashSet<>(likes.findAllByPostIdInAndUserId(page.stream().map(Post::getId).toList(), session.getUserId()).stream().map(l -> l.getPost().getId()).toList());
        List<PostResponse> result = page.stream().map(p -> response(p, liked.contains(p.getId()))).toList();
        Post last = more && !page.isEmpty() ? page.getLast() : null;
        return new PostPageResponse(result, last == null ? null : last.getCreatedAt(), last == null ? null : last.getId());
    }

    @Transactional(readOnly = true) public PostResponse get(Long id) { Post p = post(id); return response(p, likes.findByPostIdAndUserId(id, session.getUserId()).isPresent()); }

    @Transactional public PostResponse create(PostRequest request) {
        User author = user(); String role = session.getRole(); Facility facility = null;
        if ("FACILITY_OWNER".equals(role)) {
            if (request.facilityId() == null) throw bad("Choose the facility publishing this post");
            facility = facilities.findByIdAndDeletedAtIsNull(request.facilityId()).filter(f -> f.getOwner().getId().equals(author.getId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this facility"));
        } else if (!"TRAINER".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trainers and facility owners can publish posts");
        Post post = new Post(); post.setAuthor(author); post.setFacility(facility); apply(post, request, author, facility, false);
        return response(posts.saveAndFlush(post), false);
    }

    @Transactional public PostResponse update(Long id, PostRequest request) {
        Post post = post(id); requireAuthorOrAdmin(post); apply(post, request, post.getAuthor(), post.getFacility(), true);
        return response(posts.saveAndFlush(post), likes.findByPostIdAndUserId(id, session.getUserId()).isPresent());
    }
    @Transactional public void delete(Long id) { Post post = post(id); requireAuthorOrAdmin(post); post.setDeletedAt(LocalDateTime.now()); posts.saveAndFlush(post); }
    @Transactional public void like(Long id) { requireUserRole(); Post post = post(id); if (likes.findByPostIdAndUserId(id, session.getUserId()).isEmpty()) { PostLike like = new PostLike(); like.setPost(post); like.setUser(user()); likes.saveAndFlush(like); } }
    @Transactional public void unlike(Long id) { requireUserRole(); likes.findByPostIdAndUserId(id, session.getUserId()).ifPresent(likes::delete); }
    @Transactional(readOnly=true) public List<PostCommentResponse> listComments(Long postId) { post(postId); return comments.findAllByPostIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(postId).stream().map(this::commentResponse).toList(); }
    @Transactional public PostCommentResponse comment(Long postId, PostCommentRequest request) { requireUserRole(); PostComment c = new PostComment(); c.setPost(post(postId)); c.setUser(user()); c.setText(request.text().trim()); return commentResponse(comments.saveAndFlush(c)); }
    @Transactional public PostCommentResponse updateComment(Long id, PostCommentRequest request) { PostComment c = comment(id); requireCommentOwner(c); c.setText(request.text().trim()); return commentResponse(comments.saveAndFlush(c)); }
    @Transactional public void deleteComment(Long id) { PostComment c = comment(id); requireCommentOwnerOrAdmin(c); c.setDeletedAt(LocalDateTime.now()); comments.saveAndFlush(c); }

    private void apply(Post post, PostRequest request, User author, Facility facility, boolean preserveMediaWhenOmitted) {
        String text = request.text() == null ? null : request.text().trim(); if (text != null && text.isEmpty()) text = null;
        boolean mediaProvided = request.imageIds() != null || request.videoIds() != null;
        if (preserveMediaWhenOmitted && !mediaProvided) {
            if (text == null && post.getMedia().isEmpty()) throw bad("A post must contain text or media");
            post.setText(text);
            return;
        }
        List<Long> imageIds = request.imageIds() == null ? List.of() : request.imageIds();
        List<Long> videoIds = request.videoIds() == null ? List.of() : request.videoIds();
        if (imageIds.size() > 10 || new HashSet<>(imageIds).size() != imageIds.size()) throw bad("A post supports up to 10 unique images");
        if (videoIds.size() > 10 || new HashSet<>(videoIds).size() != videoIds.size()) throw bad("A post supports up to 10 unique videos");
        if (text == null && imageIds.isEmpty() && videoIds.isEmpty()) throw bad("A post must contain text or media");
        List<Image> selectedImages = imageIds.stream().map(id -> ownedImage(id, author.getId(), facility)).toList();
        List<Video> selectedVideos = videoIds.stream().map(id -> videos.findByIdAndUserIdAndDeletedAtIsNull(id, author.getId())
                .orElseThrow(() -> bad("Video does not exist or is not owned by you: " + id))).toList();
        post.setText(text); post.getMedia().clear(); int position = 0;
        for (Image image : selectedImages) { PostMedia m = new PostMedia(); m.setPost(post); m.setImage(image); m.setPosition(position++); post.getMedia().add(m); }
        for (Video video : selectedVideos) { PostMedia m = new PostMedia(); m.setPost(post); m.setVideo(video); m.setPosition(position++); post.getMedia().add(m); }
    }
    private Image ownedImage(Long id, Long userId, Facility facility) { Image i = images.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> bad("Image not found: " + id)); boolean owned = i.getUser()!=null && i.getUser().getId().equals(userId); owned |= facility != null && i.getFacility()!=null && i.getFacility().getId().equals(facility.getId()); if (!owned) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Image is not owned by this author"); return i; }
    private PostResponse response(Post p, boolean liked) { UserProfile profile = profiles.findByUserId(p.getAuthor().getId()).orElse(null); String name = p.getFacility()!=null ? p.getFacility().getName() : profile==null ? p.getAuthor().getEmail() : profile.getDisplayName()!=null && !profile.getDisplayName().isBlank() ? profile.getDisplayName() : profile.getFirstName()+" "+profile.getLastName(); String authorImageUrl = p.getFacility()!=null ? images.findFirstByFacilityIdAndTypeAndDeletedAtIsNullOrderByCreatedAtDesc(p.getFacility().getId(),ImageType.LOGO).map(image -> "/api/v1/images/"+image.getId()).orElse(null) : images.findFirstByUserIdAndTypeAndDeletedAtIsNullOrderByCreatedAtDesc(p.getAuthor().getId(),ImageType.PROFILE).map(image -> "/api/v1/images/"+image.getId()).orElse(null); List<PostMediaResponse> media=p.getMedia().stream().map(m -> m.getImage()!=null ? new PostMediaResponse(m.getId(),"IMAGE","/api/v1/images/"+m.getImage().getId(),m.getImage().getOriginalName(),m.getPosition()) : new PostMediaResponse(m.getId(),"VIDEO","/api/v1/videos/"+m.getVideo().getId(),m.getVideo().getOriginalName(),m.getPosition())).toList(); boolean manageable=p.getAuthor().getId().equals(session.getUserId())||"ADMIN".equals(session.getRole()); return new PostResponse(p.getId(),p.getFacility()==null?"TRAINER":"FACILITY",p.getAuthor().getId(),name,authorImageUrl,p.getFacility()==null?null:p.getFacility().getId(),p.getText(),media,likes.countByPostId(p.getId()),comments.countByPostIdAndDeletedAtIsNull(p.getId()),liked,manageable,p.getCreatedAt(),p.getUpdatedAt()); }
    private PostCommentResponse commentResponse(PostComment c) { UserProfile p=profiles.findByUserId(c.getUser().getId()).orElse(null); String name=p==null?c.getUser().getEmail():(p.getDisplayName()!=null&&!p.getDisplayName().isBlank()?p.getDisplayName():p.getFirstName()+" "+p.getLastName()); return new PostCommentResponse(c.getId(),c.getUser().getId(),name,c.getText(),c.getCreatedAt(),c.getUpdatedAt()); }
    private User user(){return users.findById(session.getUserId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));}
    private Post post(Long id){return posts.findById(id).filter(p->p.getDeletedAt()==null).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Post not found"));}
    private PostComment comment(Long id){return comments.findById(id).filter(c->c.getDeletedAt()==null).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Comment not found"));}
    private void requireUserRole(){if(!"USER".equals(session.getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only users can like and comment");}
    private void requireAuthorOrAdmin(Post p){if(!p.getAuthor().getId().equals(session.getUserId())&&!"ADMIN".equals(session.getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot modify this post");}
    private void requireCommentOwner(PostComment c){requireUserRole();if(!c.getUser().getId().equals(session.getUserId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot modify this comment");}
    private void requireCommentOwnerOrAdmin(PostComment c){if(!c.getUser().getId().equals(session.getUserId())&&!"ADMIN".equals(session.getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot delete this comment");}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
