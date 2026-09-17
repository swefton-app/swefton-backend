# Swefton Backend

Backend service for **Swefton**, a fitness and sports platform designed to connect users, personal trainers, gyms, sports facilities, and fitness businesses in one ecosystem.

Swefton combines discovery, coaching, facility management, memberships, bookings, QR access, staff operations, progress tracking, media, documents, notifications, and analytics into a single platform.

---

## About Swefton

Swefton is built around three main sectors:

### Users

Users can discover and interact with fitness and sports services around them.

Main capabilities include:

- Discover nearby gyms and sports facilities
- Search by activity, location, rating, popularity, and availability
- View public trainer and facility profiles
- Save favorite trainers and facilities
- Manage memberships and subscriptions
- Book classes and trainer sessions
- Access facilities using QR codes
- Receive offers and promotions
- Track personal fitness progress
- Create reminders and checklists
- Store personal progress photos and media
- Review trainers and facilities
- Participate in challenges and events

---

### Trainers

Trainers can build a professional presence and manage their clients directly through Swefton.

Main capabilities include:

- Create a professional trainer profile
- Add experience, specialties, certifications, and services
- Publish photos, videos, and posts
- Promote personal training services
- Manage subscribed clients
- Communicate with clients
- Create workout programs
- Create nutrition plans
- Track client progress
- View progress charts and training adherence
- Manage appointments and availability
- Create offers and promotions
- Add assistants with controlled permissions
- Generate professional CV documents
- Manage trainer documents
- Track client relationships and history

---

### Businesses & Facilities

Swefton allows gyms and other sports businesses to manage both their public presence and internal operations.

Supported facility types can include:

- Gyms
- Boxing clubs
- Yoga studios
- Pilates studios
- Swimming centers
- Football centers
- Tennis centers
- Martial arts centers
- Multi-sport facilities

Business capabilities include:

- Create and manage a public facility profile
- Publish photos, videos, and posts
- Manage activities offered by the facility
- Manage gym machines and facility equipment
- Manage opening hours and amenities
- Create membership plans
- Manage active and inactive members
- Manage customer history
- Register and manage staff
- Assign staff roles and permissions
- Manage staff schedules
- Manage leave requests
- Create classes and schedules
- Manage bookings and waiting lists
- Register facility devices
- Manage QR-based access
- Connect the Swefton Desktop application
- Monitor daily check-ins
- Track member growth and churn
- View daily, monthly, and yearly revenue
- Create offers and campaigns
- Track operational performance
- Manage multiple locations
- Track maintenance and facility incidents

---

# Platform Architecture

Swefton uses a shared backend architecture for all clients.

```text
                    Swefton Platform
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
       Web App         Mobile App      Desktop App
       React           React Native       .NET
          │                │                │
          └────────────────┼────────────────┘
                           │
                           ▼
                    Spring Boot API
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
      PostgreSQL          Redis          File Storage
```

The backend acts as the central source of truth for:

- Authentication
- Authorization
- User profiles
- Trainer-client relationships
- Facility ownership
- Facility staff
- Memberships
- Bookings
- Check-ins
- Documents
- Media
- Notifications
- Payments
- Analytics

---

# Technology Stack

## Backend

- Java 25
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- Flyway
- Maven

## Data

- PostgreSQL
- Redis

## Integrations

- Google Sign-In
- Email verification
- File and media storage
- QR access system
- Desktop device integration

## Monitoring

- Spring Boot Actuator

---

# Backend Module Structure

The backend follows a modular domain-oriented architecture.

```text
src/main/java/com/swefton/backend/
│
├── config/
├── infrastructure/
├── security/
│
└── modules/
    │
    ├── auth/
    ├── user/
    ├── facility/
    ├── activity/
    ├── machine/
    ├── membership/
    ├── booking/
    ├── checkin/
    ├── device/
    ├── document/
    ├── notification/
    ├── payment/
    └── admin/
```

Each module can contain:

```text
controller/
service/
repository/
entity/
dto/
enums/
mapper/
```

depending on the responsibilities of the module.

---

# Core Domain Model

## User

`User` is the main identity used across the entire platform.

```text
User
│
├── UserProfile
├── UserPreferences
├── TrainerProfile
├── FacilityOwnership
└── FacilityStaff
```

A single account can participate in different parts of the platform depending on its role and relationships.

---

## Trainer

A trainer is still a Swefton user, but with additional professional data.

```text
User
 │
 ▼
TrainerProfile
 │
 ├── Services
 ├── Clients
 ├── Workout Plans
 ├── Nutrition Plans
 ├── Availability
 ├── Documents
 ├── Media
 └── Progress Tracking
```

Trainer-client relationships control access to private coaching information.

---

## Facility

A facility represents a fitness or sports business.

```text
Facility
│
├── Owners
├── Staff
├── Activities
├── Machines
├── Membership Plans
├── Members
├── Classes
├── Bookings
├── Devices
├── Check-ins
├── Media
├── Amenities
└── Opening Hours
```

The same model can support different types of sports businesses.

---

# Gym Machines

Gym training machines are represented separately from the facility.

```text
Facility
    │
    ▼
FacilityMachine
    │
    ▼
Machine
```

Example:

```text
Power Gym
│
├── Chest Press × 3
├── Leg Press × 2
├── Treadmill × 10
├── Lat Pulldown × 2
└── Smith Machine × 2
```

The relation can also store information such as:

- Quantity
- Brand
- Model
- Availability
- Maintenance status

---

# Membership System

Facilities can create different membership plans.

Examples:

```text
Monthly Membership
3 Month Membership
Annual Membership
Student Membership
Family Membership
Day Pass
Class Pack
```

Memberships connect a user with a facility and control access rules.

```text
User
  │
  ▼
Membership
  │
  ▼
Facility
```

The system can track:

- Start date
- Expiration date
- Status
- Renewal
- Remaining entries
- Plan
- Payment history

---

# QR Check-In System

Swefton includes a QR-based access system for facilities.

```text
User Mobile App
      │
      ▼
Personal QR
      │
      ▼
Facility Scanner / Desktop
      │
      ▼
Spring Boot
      │
      ├── Validate User
      ├── Validate Facility
      ├── Validate Membership
      ├── Validate Device
      └── Validate Access Rules
      │
      ▼
APPROVED / DENIED
```

Every scan can create a check-in record.

```text
CheckIn
├── User
├── Facility
├── Device
├── Timestamp
├── Status
└── Reason
```

This allows businesses to analyze:

- Daily check-ins
- Monthly attendance
- Peak hours
- Active members
- Facility occupancy

---

# Facility Staff Management

Facility staff members use their own Swefton accounts.

```text
Facility
   │
   ▼
FacilityStaff
   │
   ▼
User
```

Staff roles may include:

```text
Manager
Receptionist
Trainer
Instructor
Administrative Staff
General Staff
```

Staff functionality can include:

- Working schedules
- Shift assignments
- Leave requests
- Attendance
- Operational checklists
- Role-based permissions

---

# Trainer Client Management

Trainers can manage their active clients through the platform.

```text
Trainer
   │
   ▼
TrainerClient
   │
   ▼
User
```

The trainer can manage:

- Workout plans
- Nutrition plans
- Client notes
- Measurements
- Progress photos
- Weight progress
- Training adherence
- Session history
- Goals
- Progress charts

The client can also view their own progress through the mobile app.

---

# Progress Tracking

Swefton can track fitness progress over time.

Examples:

```text
Weight
Body measurements
Workout performance
Attendance
Training consistency
Personal records
Progress photos
```

Historical data should be stored as timestamped records.

Example:

```text
UserProgress
│
├── Date
├── Weight
├── Chest
├── Waist
├── Arms
├── Legs
└── Notes
```

This allows the frontend to generate progress charts and trends.

---

# Documents

Swefton includes a generic document system.

Documents can be associated with users and trainers.

Examples:

```text
CV
Certificate
License
Diploma
Trainer qualification
Other documents
```

A trainer can also generate a professional CV through a questionnaire-based CV builder.

Flow:

```text
CV Questions
      │
      ▼
Choose Template
      │
      ▼
Preview
      │
      ▼
Generate PDF
      │
      ▼
Store as User Document
```

---

# Authentication

Swefton uses its own authentication system.

Supported authentication methods include:

```text
Email + Password
Google Sign-In
```

The backend then issues Swefton authentication tokens used by all clients.

Authentication architecture:

```text
Email / Google
      │
      ▼
Swefton Authentication
      │
      ▼
Access Token
      +
Refresh Session
      │
      ▼
Web / Mobile / Desktop
```

---

# Roles

Main platform roles:

```text
USER
TRAINER
FACILITY_OWNER
ADMIN
```

Facility-specific permissions are handled separately.

For example:

```text
User
Global Role: TRAINER

Facility #1
Role: TRAINER

Facility #2
Role: MANAGER
```

This allows one user to have different responsibilities in different facilities.

---

# Notifications

Swefton can send notifications for events such as:

- Booking confirmation
- Booking cancellation
- Membership expiration
- Membership renewal
- New trainer message
- New workout plan
- Trainer updates
- Facility offers
- Promotions
- Staff schedule updates
- Leave request decisions
- Payment status
- Account security events

Notification channels can include:

```text
In-App
Push Notification
Email
```

---

# Analytics

## User Analytics

Users can view:

- Training frequency
- Progress history
- Weight changes
- Goal progress
- Workout consistency
- Membership activity

## Trainer Analytics

Trainers can view:

- Active clients
- New clients
- Client retention
- Client adherence
- Session activity
- Revenue
- Progress status

## Business Analytics

Businesses can view:

- Active members
- New members
- Cancelled memberships
- Daily check-ins
- Monthly check-ins
- Peak hours
- Revenue
- Member growth
- Member churn
- Class occupancy
- Staff activity

---

# Frontend Applications

Swefton uses multiple client applications.

## Web

Built with React.

Used for:

- Public profiles
- Facility management
- Trainer management
- Business dashboards
- Administration
- Onboarding

## Mobile

Built with React Native / Expo.

Used primarily by:

- Users
- Trainers

Main mobile features include:

- Discovery
- QR access
- Bookings
- Progress tracking
- Coaching
- Notifications
- Messaging
- Media

## Desktop

Built with .NET.

Designed primarily for facility operations.

Examples:

- Reception
- QR scanners
- Member validation
- Check-in confirmation
- Facility monitoring

---

# Development Philosophy

The backend follows several core principles.

### Modular

Each business area owns its own logic.

### API First

Web, Mobile, and Desktop communicate through the same backend APIs.

### Shared Business Logic

Business rules are implemented in the backend instead of being duplicated across clients.

### Secure by Default

Access is validated using both global roles and resource relationships.

### Historical Data

Important actions such as:

- Check-ins
- Payments
- Membership changes
- Progress
- Bookings

should preserve history.

### Scalable

The architecture should support the transition from a local MVP to a production platform with multiple facilities, trainers, users, and locations.

---

# Current Project Direction

The current development focus includes:

```text
Authentication
User management
Trainer profiles
Facility management
Activities
Gym machines
Facility staff
Memberships
QR check-in
Documents
Media
Notifications
Bookings
```

Future modules can include:

```text
Payments
Advanced analytics
Messaging
Community feed
Challenges
Wearable integrations
Corporate memberships
Referral programs
Multi-location business analytics
AI-assisted recommendations
```

---

# Project Structure

Swefton is designed as an ecosystem rather than a single-purpose fitness application.

```text
                 SWEFTON

        ┌──────────┼──────────┐
        │          │          │
        ▼          ▼          ▼
      USER      TRAINER    BUSINESS
        │          │          │
        └──────────┼──────────┘
                   │
                   ▼
              SHARED PLATFORM
                   │
        ┌──────────┼──────────┐
        │          │          │
        ▼          ▼          ▼
    MEMBERSHIPS  BOOKINGS  QR ACCESS
        │          │          │
        ├──────────┼──────────┤
        │          │          │
     PAYMENTS   MEDIA     NOTIFICATIONS
        │          │          │
        └──────────┼──────────┘
                   │
                   ▼
               ANALYTICS
```

---

# Project Goal

The goal of Swefton is to create a complete digital ecosystem where:

- Users discover and manage their fitness journey
- Trainers grow their professional business and manage clients
- Facilities manage memberships, staff, access, and operations
- All participants interact through one connected platform

Swefton is designed to make fitness and sports services easier to discover, manage, track, and grow.

---

## Swefton

**Move. Connect. Grow.**
