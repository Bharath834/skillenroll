# 02_REQUIREMENTS.md — SkillEnroll

> **Document Version:** 1.0  
> **Author:** Senior Software Architect  
> **Framework:** TrainingMug AI Development Framework  
> **Status:** ✅ Approved  
> **Related Doc:** [01_PROJECT_CONTEXT.md](./01_PROJECT_CONTEXT.md)

---

## Table of Contents

1. [User Roles](#1-user-roles)
2. [Functional Requirements](#2-functional-requirements)
3. [User Stories](#3-user-stories)
4. [Acceptance Criteria](#4-acceptance-criteria)
5. [Non-Functional Requirements](#5-non-functional-requirements)
6. [Out of Scope Features](#6-out-of-scope-features)

---

## 1. User Roles

SkillEnroll defines **four distinct user roles**, each with a specific set of permissions and responsibilities.

### 1.1 Guest (Unauthenticated Visitor)

A user who has not signed in. Guests have the most restricted access — they can only browse the public catalog and sign up.

**Capabilities:**
- View the public program catalog (title, summary, thumbnail)
- Search and filter programs by category, keyword, or skill level
- View detailed program information (syllabus overview, instructor, duration, prerequisites)
- Register for a new account (as a Learner or Instructor)
- Log in to an existing account

**Restrictions:**
- Cannot enroll in programs
- Cannot access learning content or assessments
- Cannot view learner dashboards or progress

---

### 1.2 Learner

An authenticated user who enrolls in and completes skill programs.

**Capabilities:**
- All Guest capabilities (while signed in)
- Enroll in and unenroll from programs (within cancellation window)
- Access learning content: modules, lessons, attached materials
- Take assessments (quizzes, assignments) and view grades
- Track personal progress across all enrolled programs
- Download certificates of completion
- Update personal profile (name, avatar, preferences)
- Receive notifications (enrollment confirmation, due dates, grades)

**Restrictions:**
- Cannot create or edit programs
- Cannot view other learners' data
- Cannot access admin or instructor dashboards

---

### 1.3 Instructor / Content Creator

An authenticated user who designs, publishes, and manages skill programs and assessments.

**Capabilities:**
- All Learner capabilities (optional — instructor may also be enrolled as a learner)
- Create new skill programs with structured curricula (modules → lessons)
- Upload learning materials (documents, videos, links, embedded content)
- Create assessments (quizzes with multiple question types, assignments)
- Set passing criteria and grading rubrics
- Publish, unpublish, or archive programs
- Manage enrolled learners — view rosters, monitor progress
- Grade learner submissions and provide feedback
- Update program content after publication (with versioning or draft mode)

**Restrictions:**
- Cannot delete other instructors' programs
- Cannot access system-level admin features (user management, global reports)
- Cannot modify platform configuration

---

### 1.4 Admin / Manager

An authenticated user with full system oversight. Typically an organization administrator.

**Capabilities:**
- All capabilities of other roles (system-wide)
- Manage users: create, edit, suspend, or delete accounts; assign roles
- Manage all programs: review, approve/reject, feature, or remove
- View system-wide reports and analytics (completion rates, engagement, trends)
- Configure platform settings (enrollment policies, notification templates, branding)
- Audit logs for security and compliance
- Approve instructor applications or program publications (if moderation is enabled)

**Restrictions:**
- (No restrictions beyond ethical use and audit trails)

---

## 2. Functional Requirements

Functional requirements are organized by **domain module**, matching the bounded contexts defined in the architecture.

### 2.1 Authentication & Authorization Module (AUTH)

| ID | Requirement | Detail | Maps To |
|----|-------------|--------|---------|
| FR‑AUTH‑01 | **User Registration** | Users shall register with email, password, full name, and role selection (Learner or Instructor). Email verification via OTP is required before first login. | BO‑4 |
| FR‑AUTH‑02 | **User Login** | Users shall authenticate with email and password. Spring Security + JWT issues an access token (15 min expiry) and a refresh token (7 day expiry). | BO‑4 |
| FR‑AUTH‑03 | **Password Reset** | Users shall request a password reset via email. A time-limited reset link is sent. | BO‑4 |
| FR‑AUTH‑04 | **Role‑Based Access Control** | Every API endpoint shall enforce access based on the user's role (Guest, Learner, Instructor, Admin). Unauthorized requests return HTTP 403. | BO‑4 |
| FR‑AUTH‑05 | **Profile Management** | Authenticated users shall update their profile (name, avatar, bio, contact info). | — |
| FR‑AUTH‑06 | **Session Management** | Users shall be able to view and revoke active sessions (logout from other devices). | BO‑4 |
| FR‑AUTH‑07 | **Account Deactivation** | Users shall be able to deactivate their own account. Admins may reactivate it. | BO‑4 |

### 2.2 Catalog Module (CAT)

| ID | Requirement | Detail | Maps To |
|----|-------------|--------|---------|
| FR‑CAT‑01 | **Browse Programs** | All users (including guests) shall browse the public program catalog with pagination. | BO‑1 |
| FR‑CAT‑02 | **Search Programs** | Users shall search programs by title, description, instructor name, or keyword. Search results rank by relevance (title match > description match). | BO‑1 |
| FR‑CAT‑03 | **Filter & Sort** | Users shall filter programs by category, skill level (Beginner / Intermediate / Advanced), duration range, and price (free vs paid). Sort by newest, most popular, or highest rated. | BO‑1 |
| FR‑CAT‑04 | **Program Detail View** | Users shall view a program's full details: title, description, instructor bio, syllabus outline, prerequisites, duration, estimated effort, rating, and enrolled count. | BO‑1, BO‑2 |
| FR‑CAT‑05 | **Program Thumbnail & Preview** | Each program shall display a thumbnail image and a short summary in card format. Optionally, a preview video or sample lesson. | BO‑1 |
| FR‑CAT‑06 | **Category Management** | Admins shall create, edit, and delete program categories. Instructors shall assign categories when creating a program. | BO‑1 |

### 2.3 Program Management Module (PM)

| ID | Requirement | Detail | Maps To |
|----|-------------|--------|---------|
| FR‑PM‑01 | **Create Program** | Instructors shall create a new program by providing title, description, category, skill level, duration, prerequisites, thumbnail, and price (free or paid). | BO‑1, BO‑5 |
| FR‑PM‑02 | **Curriculum Builder** | Instructors shall define a structured curriculum: a program contains one or more modules; each module contains one or more lessons. Lessons may have attached materials (documents, videos, links). | BO‑5 |
| FR‑PM‑03 | **Draft / Publish Workflow** | Programs shall support a draft state (editable, not visible to learners), a published state (visible and enrollable), and an archived state (visible but not enrollable). | BO‑1, BO‑5 |
| FR‑PM‑04 | **Program Update & Versioning** | Instructors shall edit published programs. Changes shall create a new draft version without affecting currently enrolled learners until explicitly published. | BO‑5 |
| FR‑PM‑05 | **Program Approval (Admin)** | Optionally, admins may require published programs to go through an approval queue. Learners see only approved programs. | BO‑4 |
| FR‑PM‑06 | **Program Deletion** | Instructors may delete unpublished programs. Published programs shall be archived instead of deleted to preserve enrolled learner data. | BO‑5 |

### 2.4 Enrollment Module (ENR)

| ID | Requirement | Detail | Maps To |
|----|-------------|--------|---------|
| FR‑ENR‑01 | **Enroll in Program** | Learners shall enroll in a published program with a single click. Enrollment creates a learning record and grants immediate content access. | BO‑2 |
| FR‑ENR‑02 | **Enrollment Confirmation** | Upon successful enrollment, learners shall receive an on‑screen confirmation and an email notification. | BO‑2 |
| FR‑ENR‑03 | **Unenroll / Withdraw** | Learners may unenroll from a program within a configurable cancellation window (default: 7 days). Progress data is retained for 30 days in case of re‑enrollment. | BO‑2 |
| FR‑ENR‑04 | **Enrollment Capacity** | Instructors may set a maximum enrollment capacity per program. Once reached, new learners see a "Course Full" notice with an optional waitlist. | — |
| FR‑ENR‑05 | **Enrollment Dashboard** | Instructors and admins shall view an enrollment roster for each program, including learner names, enrollment dates, and current progress percentage. | BO‑3 |
| FR‑ENR‑06 | **Waitlist** | When a program is at capacity, learners may join a waitlist. When a slot opens, the next learner on the list is notified and given 48 hours to enroll before the slot passes to the next. | — |

### 2.5 Learning Module (LRN)

| ID | Requirement | Detail | Maps To |
|----|-------------|--------|---------|
| FR‑LRN‑01 | **Program Player / Learning View** | Enrolled learners shall access a dedicated learning interface that displays the curriculum, lesson content, and progress indicators per module. | BO‑3 |
| FR‑LRN‑02 | **Lesson Content** | Lessons shall support rich content: formatted text, embedded video (YouTube/Vimeo), PDF downloads, external links, and code snippets. Content is rendered in the browser. | BO‑5 |
| FR‑LRN‑03 | **Progress Tracking** | The system shall automatically mark lessons as "completed" when the learner views the content (for read‑only lessons) or submits an assessment (for graded lessons). Instructors may manually override completion status. | BO‑3 |
| FR‑LRN‑04 | **Module Completion** | When all lessons in a module are completed, the module is marked complete. The next module is unlocked (sequential mode) or accessible immediately (self‑paced mode), depending on program configuration. | BO‑3 |
| FR‑LRN‑05 | **Bookmark / Resume** | Learners shall bookmark their last‑viewed lesson. The learning view resumes from the last position on next visit. | — |
| FR‑LRN‑06 | **Notes & Highlights** | Learners may add personal notes to any lesson and highlight text. Notes are private and persisted across sessions. | — |

### 2.6 Assessment Module (ASM)

| ID | Requirement | Detail | Maps To |
|----|-------------|--------|---------|
| FR‑ASM‑01 | **Quiz Creation** | Instructors shall create quizzes with multiple question types: multiple choice (single/multi‑select), true/false, short answer, and fill‑in‑the‑blank. Questions may have point values and optional explanations. | BO‑5 |
| FR‑ASM‑02 | **Quiz Configuration** | Instructors shall configure a time limit, passing score threshold, number of attempts allowed, and whether answers are revealed after submission. | BO‑5 |
| FR‑ASM‑03 | **Take Quiz** | Learners shall take quizzes within the learning interface. A timer is displayed for time‑limited quizzes. Auto‑submit occurs when time expires. | BO‑3 |
| FR‑ASM‑04 | **Auto‑Grading** | Multiple choice, true/false, and fill‑in‑the‑blank questions shall be auto‑graded immediately upon submission. Short answer questions are flagged for manual grading. | BO‑3 |
| FR‑ASM‑05 | **Assignment Submission** | Instructors may create assignments requiring file uploads or written responses. Learners submit via the platform. | BO‑5 |
| FR‑ASM‑06 | **Manual Grading & Feedback** | Instructors shall review and grade assignments and short‑answer questions. They provide a numeric score and written feedback. | BO‑5 |
| FR‑ASM‑07 | **Grade Book** | Learners shall view their scores for each completed assessment. Instructors and admins shall view aggregate scores per program, per learner. | BO‑3, BO‑6 |

### 2.7 Progress & Reporting Module (RPT)

| ID | Requirement | Detail | Maps To |
|----|-------------|--------|---------|
| FR‑RPT‑01 | **Learner Dashboard** | Learners shall see a personal dashboard: enrolled programs, progress per program (%), upcoming deadlines, recent activity, and certificates earned. | BO‑3 |
| FR‑RPT‑02 | **Program‑Level Progress** | Within a program, learners shall see per‑module completion status (not started, in progress, completed) and assessment scores. | BO‑3 |
| FR‑RPT‑03 | **Instructor Reports** | Instructors shall view per‑program analytics: total enrolled, average progress %, average quiz score, completion rate, and a list of learners at risk (behind schedule). | BO‑6 |
| FR‑RPT‑04 | **Admin Reports** | Admins shall view system‑wide reports: total users, active learners, programs published, overall completion rates, trends over time, and top‑performing programs. | BO‑6 |
| FR‑RPT‑05 | **Report Export** | Admins and instructors shall export reports as CSV or PDF. | BO‑6 |
| FR‑RPT‑06 | **Certificate Generation** | Upon completing all modules and passing all required assessments in a program, learners shall receive a digitally signed certificate of completion (PDF). Certificates include learner name, program title, completion date, and a unique verification URL. | BO‑3 |

### 2.8 Notification Module (NTF)

| ID | Requirement | Detail | Maps To |
|----|-------------|--------|---------|
| FR‑NTF‑01 | **In‑App Notifications** | Users shall receive in‑app notifications for key events: enrollment confirmation, grade posted, new lesson available, certificate issued, program published. Notifications appear in a dropdown bell icon. | BO‑2, BO‑3 |
| FR‑NTF‑02 | **Email Notifications** | Users shall receive email notifications for critical events: welcome email, password reset, enrollment confirmation, grade posted (if opted in). Users may configure email preferences. | BO‑2 |
| FR‑NTF‑03 | **Notification Preferences** | Users shall opt in/out of specific notification types (e.g., "grade posted", "new content") via their profile settings. | — |

### 2.9 Admin Module (ADM)

| ID | Requirement | Detail | Maps To |
|----|-------------|--------|---------|
| FR‑ADM‑01 | **User Management** | Admins shall view, search, filter, create, edit, suspend, or delete users. Admins may change any user's role. | BO‑4 |
| FR‑ADM‑02 | **Program Moderation** | Admins shall review programs submitted for approval. They may approve, reject (with reason), or request changes. | BO‑1 |
| FR‑ADM‑03 | **System Configuration** | Admins shall configure system‑wide settings: enrollment policies, default cancellation window, certificate template, platform name, branding (logo, colors). | BO‑4 |
| FR‑ADM‑04 | **Audit Log** | The system shall log all admin actions (user role changes, program deletions, configuration changes) with timestamps and actor IDs. Logs are viewable only by admins. | BO‑4 |
| FR‑ADM‑05 | **Bulk Operations** | Admins shall perform bulk user operations: invite users via CSV upload, assign roles in bulk, suspend/activate multiple accounts. | BO‑4 |

---

## 3. User Stories

### 3.1 Guest (Unauthenticated)

| ID | Story |
|----|-------|
| US‑GST‑01 | As a **guest**, I want to **browse the program catalog without signing up**, so that **I can evaluate whether the platform offers programs relevant to my goals**. |
| US‑GST‑02 | As a **guest**, I want to **search and filter programs by category and skill level**, so that **I can quickly find programs that match my interests**. |
| US‑GST‑03 | As a **guest**, I want to **view program details including syllabus and instructor info**, so that **I can make an informed decision before registering**. |
| US‑GST‑04 | As a **guest**, I want to **register as a learner or instructor**, so that **I can access enrollment and content features**. |

### 3.2 Learner

| ID | Story |
|----|-------|
| US‑LRN‑01 | As a **learner**, I want to **enroll in a program with one click**, so that **I can start learning immediately without friction**. |
| US‑LRN‑02 | As a **learner**, I want to **access program content in a structured, sequential interface**, so that **I can follow the curriculum from start to finish**. |
| US‑LRN‑03 | As a **learner**, I want to **track my progress across all programs on a personal dashboard**, so that **I can see how far I've come and what's remaining**. |
| US‑LRN‑04 | As a **learner**, I want to **take quizzes and receive instant feedback on auto‑graded questions**, so that **I can assess my understanding as I go**. |
| US‑LRN‑05 | As a **learner**, I want to **submit assignments and receive instructor feedback**, so that **I can improve through personalized guidance**. |
| US‑LRN‑06 | As a **learner**, I want to **download a certificate after completing a program**, so that **I can showcase my achievement professionally**. |
| US‑LRN‑07 | As a **learner**, I want to **add personal notes to lessons**, so that **I can capture key takeaways for later review**. |
| US‑LRN‑08 | As a **learner**, I want to **receive notifications when grades are posted or new content is available**, so that **I stay engaged without constantly checking**. |

### 3.3 Instructor

| ID | Story |
|----|-------|
| US‑INS‑01 | As an **instructor**, I want to **create a new program with a structured curriculum of modules and lessons**, so that **I can deliver a well‑organized learning experience**. |
| US‑INS‑02 | As an **instructor**, I want to **upload lesson materials including videos, documents, and external links**, so that **I can provide diverse learning resources**. |
| US‑INS‑03 | As an **instructor**, I want to **create quizzes with multiple question types and configure passing criteria**, so that **I can assess learner knowledge effectively**. |
| US‑INS‑04 | As an **instructor**, I want to **publish and unpublish my programs**, so that **I can control when content is available to learners**. |
| US‑INS‑05 | As an **instructor**, I want to **view enrolled learners and their progress**, so that **I can identify who needs additional support**. |
| US‑INS‑06 | As an **instructor**, I want to **grade learner submissions and provide written feedback**, so that **learners can understand their performance**. |
| US‑INS‑07 | As an **instructor**, I want to **update program content without disrupting currently enrolled learners**, so that **I can improve the program iteratively**. |
| US‑INS‑08 | As an **instructor**, I want to **see aggregate analytics for my programs (enrollment, completion rates, average scores)**, so that **I can measure the effectiveness of my content**. |

### 3.4 Admin

| ID | Story |
|----|-------|
| US‑ADM‑01 | As an **admin**, I want to **manage all user accounts (create, edit, suspend)**, so that **I can maintain a healthy platform community**. |
| US‑ADM‑02 | As an **admin**, I want to **review and approve instructor‑published programs**, so that **I can ensure quality standards are met**. |
| US‑ADM‑03 | As an **admin**, I want to **view system‑wide analytics and export reports**, so that **I can make data‑driven decisions about platform growth**. |
| US‑ADM‑04 | As an **admin**, I want to **configure platform settings (branding, policies, templates)**, so that **the platform aligns with organizational identity and rules**. |
| US‑ADM‑05 | As an **admin**, I want to **view an audit log of admin actions**, so that **I can maintain security and compliance**. |
| US‑ADM‑06 | As an **admin**, I want to **invite users in bulk via CSV upload**, so that **I can onboard teams efficiently**. |

---

## 4. Acceptance Criteria

### 4.1 Guest Browsing & Search (FR‑CAT‑01, FR‑CAT‑02, FR‑CAT‑03)

```
Given  I am a guest on the SkillEnroll homepage (not logged in)
When   I land on the catalog page
Then   I see a grid of program cards, each showing:
       - Program thumbnail image
       - Program title
       - Instructor name
       - Skill level badge (Beginner / Intermediate / Advanced)
       - Brief summary (≤ 100 characters)
And    The catalog is paginated with 20 programs per page
And    I can navigate between pages

Given  I am a guest on the catalog page
When   I type a search term into the search bar
Then   Results update in real‑time (debounced, ≥ 300 ms) as I type
And    Results match by title (highest priority), description, instructor name, or keyword
And    If no results match, I see "No programs found for [search term]"
And    A "Clear filter" button resets the search

Given  I am a guest on the catalog page
When   I select a filter option (e.g., category "Web Development", skill level "Beginner")
Then   The catalog updates to show only matching programs
And    Active filters are displayed as removable chips/tags above the results

Given  I am a guest on the catalog page
When   I select a sort option (e.g., "Most Popular")
Then   Results are reordered accordingly
And    The current sort option is visually indicated

Given  I am a guest on the catalog page
And    The catalog contains exactly 45 programs
When   I browse all pages
Then   I see programs 1–20 on page 1, 21–40 on page 2, and 41–45 on page 3
And    Each page displays the correct count (e.g., "Showing 1–20 of 45")
```

### 4.2 User Registration Flow (FR‑AUTH‑01)

```
Given  I am a guest on the registration page
When   I submit the registration form with valid email, password, name, and role
Then   I receive a success message "Please verify your email"
And    A verification email with a 6‑digit OTP is sent to the provided email
And    An unverified user record is created in the database

Given  I enter an invalid email format
When   I submit the registration form
Then   I see an inline error "Please enter a valid email address"
And    The form is not submitted

Given  I enter a password shorter than 8 characters
When   I submit the registration form
Then   I see an inline error "Password must be at least 8 characters"
And    The form is not submitted

Given  I enter an email that is already registered
When   I submit the registration form
Then   I see an inline error "An account with this email already exists"
And    The form is not submitted

Given  I enter the OTP sent to my email
When   I submit the OTP verification form
Then   My account is marked as verified
And    I am redirected to the login page with a success message
```

### 4.2 Program Enrollment (FR‑ENR‑01)

```
Given  I am a logged‑in learner
And    I am viewing a published program's detail page
When   I click the "Enroll Now" button
Then   The system creates an enrollment record with status "active"
And    I see a success confirmation "Successfully enrolled in [Program Title]"
And    The program appears in my learner dashboard
And    I receive an in‑app notification
And    I receive an email confirmation (if email notifications are enabled)

Given  I am a logged‑in learner
And    The program has reached its maximum enrollment capacity
When   I click the "Enroll Now" button
Then   I see a message "This program is full. Join the waitlist?"
And    The "Enroll Now" button changes to "Join Waitlist"

Given  I am not logged in
When   I click the "Enroll Now" button
Then   I am redirected to the login page
And    After login, I am redirected back to the program detail page
```

### 4.3 Curriculum Builder (FR‑PM‑02)

```
Given  I am an instructor creating a new program
When   I navigate to the curriculum builder
Then   I see an empty module list with an "Add Module" button

Given  I click "Add Module"
When   I enter a module title and optional description
Then   A new module is added to the curriculum
And    I can add lessons within that module

Given  I click "Add Lesson" within a module
When   I enter lesson title, select content type (text/video/document/link), and provide content
Then   The lesson is added to the module
And    I can reorder lessons within a module via drag‑and‑drop
And    I can reorder modules via drag‑and‑drop

Given  I have added at least one module with at least one lesson
When   I click "Save Draft" or "Publish"
Then   The curriculum structure is persisted to the database
```

### 4.4 Quiz Auto‑Grading (FR‑ASM‑04)

```
Given  I am a learner taking a quiz with multiple‑choice questions
When   I submit all my answers before the timer expires
Then   Each multiple‑choice question is graded instantly
And    I see my total score immediately after submission
And    I see which questions I answered correctly/incorrectly (if configured)
And    For incorrect answers, the correct answer is shown (if configured)

Given  I am taking a time‑limited quiz
And    The timer reaches zero
When   The system auto‑submits
Then   All answered questions are graded
And    Unanswered questions are marked as incorrect (zero points)

Given  The quiz includes short‑answer questions
When   I submit the quiz
Then   The auto‑graded portion is scored immediately
And    The short‑answer questions show "Awaiting Instructor Grading"
And    I receive a notification when grades are posted
```

### 4.5 Learner Dashboard (FR‑RPT‑01)

```
Given  I am a logged‑in learner
When   I navigate to "My Dashboard"
Then   I see:
       - A summary card showing total programs enrolled, completed, in progress
       - A list of my enrolled programs with title, progress bar (%), and last accessed date
       - A section for "Upcoming Deadlines" (if any assessments are due within 7 days)
       - A section for "Recent Activity" (last 5 actions: enrolled, completed lesson, scored quiz)
       - A section for "Certificates Earned" with download links

Given  I click on a program card in the dashboard
When   I am redirected to the program's learning view
Then   The curriculum is displayed with checkmarks on completed modules/lessons

Given  I have no enrolled programs
When   I view my dashboard
Then   I see an empty state message "You haven't enrolled in any programs yet"
And    A CTA button "Browse Catalog" links to the catalog page
```

### 4.6 Certificate Generation (FR‑RPT‑06)

```
Given  I am a learner who has completed all modules
And    I have passed all required assessments in a program
When   I navigate to the program's completion page
Then   I see a "Download Certificate" button
And    Clicking it downloads a PDF certificate

Given  The downloaded certificate
Then   It contains:
       - Learner's full name
       - Program title
       - Completion date (formatted)
       - Program duration (if configured)
       - A unique certificate ID (UUID)
       - A verification URL: https://app.skillenroll.app/verify/<certificate_id>
       - Digital signature hash (SHA‑256 of learner ID + program ID + completion date)
       - Platform branding (logo, org name)

Given  I am a third party with a certificate verification URL
When   I visit the verification URL
Then   I see whether the certificate is valid, revoked, or not found
```

### 4.7 In-App Notifications (FR‑NTF‑01)

```
Given  I am a logged‑in learner
When   I enroll in a program
Then   A notification bell icon in the header shows a red badge with count "1"
And    Clicking the bell opens a dropdown showing: "You enrolled in [Program Title]"
And    The notification includes a timestamp (e.g., "2 minutes ago")

Given  I have 5 unread notifications
When   I click the bell icon
Then   I see the 5 most recent notifications, ordered newest first
And    Each notification shows:
       - Icon representing the event type (enrollment, grade, certificate, etc.)
       - Message text
       - Relative timestamp
And    Unread notifications are visually distinct from read ones (bold text)

Given  I click on a single notification
When   The notification is processed
Then   The notification is marked as read
And    The badge count decrements
And    I am navigated to the relevant page (e.g., grade notification → assessment review page)

Given  I click "Mark all as read"
When   The action is confirmed
Then   All notifications in the dropdown are marked as read
And    The badge count disappears
```

### 4.8 Email Notifications (FR‑NTF‑02)

```
Given  I have just registered with a valid email
When   Registration is successful
Then   A welcome email is sent to my registered email address within 60 seconds
And    The email contains:
       - A greeting with my full name
       - A confirmation that my account was created
       - A link to log in
       - Platform branding (logo, organization name)

Given  I have just enrolled in a program
When   Enrollment is confirmed
Then   An enrollment confirmation email is sent within 60 seconds
And    The email contains:
       - Program title and instructor name
       - A link to start learning
       - Estimated program duration

Given  I am a learner with email notifications enabled
When   An instructor posts a grade for my submission
Then   A grade notification email is sent within 5 minutes
And    The email contains:
       - Program and assessment name
       - My score
       - Instructor feedback summary (if any)
       - A link to view the full grade details

Given  I have disabled "grade" email notifications in my preferences
When   An instructor posts a grade for my submission
Then   No grade notification email is sent
But    I still receive the in‑app notification
```

### 4.9 Admin User Management (FR‑ADM‑01)

```
Given  I am an admin on the User Management page
When   I view the user list
Then   I see paginated results with columns: Name, Email, Role, Status (Active/Suspended/Unverified), Last Login, Actions
And    I can search by name or email
And    I can filter by role or status

Given  I select "Suspend" on an active user
When   I confirm the suspension in the confirmation dialog
Then   The user's status changes to "Suspended"
And    The user cannot log in
And    A log entry is created in the audit log

Given  I select "Edit" on a user
When   I modify their role from Learner to Instructor
Then   The user's role is updated
And    A log entry is created in the audit log with actor ID
```

---

## 5. Non-Functional Requirements

### 5.1 Performance

| ID | Requirement | Target | Measurement |
|----|-------------|--------|-------------|
| NFR‑PERF‑01 | **Page Load Time** | All static pages (catalog, dashboard, learning view) shall load within **2 seconds** on a standard broadband connection (10 Mbps) | Lighthouse / Web Vitals (FCP, LCP) |
| NFR‑PERF‑02 | **API Response Time** | 95 % of API requests shall respond within **500 ms** (p99 < 2 seconds) under normal load | JMeter / K6 |
| NFR‑PERF‑03 | **Concurrent Users** | The system shall support **500 concurrent active users** without degradation | Load testing |
| NFR‑PERF‑04 | **Search Response Time** | Catalog search shall return results within **1 second** for up to 1,000 programs | Search endpoint timing |
| NFR‑PERF‑05 | **Report Generation** | Reports covering ≤ 10,000 records shall generate within **5 seconds**; larger reports within **30 seconds** | Report endpoint timing |
| NFR‑PERF‑06 | **Certificate Generation** | Certificate PDF generation shall complete within **3 seconds** per request | Certificate endpoint timing |

### 5.2 Availability & Reliability

| ID | Requirement | Target | Measurement |
|----|-------------|--------|-------------|
| NFR‑AVL‑01 | **Uptime** | The platform shall achieve **99.5 % uptime** during business hours (08:00 – 20:00) excluding scheduled maintenance | Uptime monitoring |
| NFR‑AVL‑02 | **Planned Maintenance** | Scheduled maintenance windows shall be communicated **48 hours in advance** and not exceed **2 hours per month** | Maintenance logs |
| NFR‑AVL‑03 | **Data Backup** | Database shall be backed up **daily** with a Recovery Point Objective (RPO) of ≤ 24 hours | Backup verification |
| NFR‑AVL‑04 | **Disaster Recovery** | Recovery Time Objective (RTO) shall be ≤ **4 hours** for full platform restoration | DR drill |

### 5.3 Security

| ID | Requirement | Detail |
|----|-------------|--------|
| NFR‑SEC‑01 | **HTTPS Enforcement** | All communication between client and server shall use TLS 1.2+ with valid SSL certificates. HTTP requests shall redirect to HTTPS. |
| NFR‑SEC‑02 | **Authentication** | User authentication shall use JWT with access tokens (15 min expiry) and refresh tokens (7 day expiry, rotation enabled). Passwords hashed with BCrypt (cost factor ≥ 12). |
| NFR‑SEC‑03 | **Authorization** | All API endpoints shall enforce role‑based access control at the Spring Security layer. Role checks shall be server‑side only — never rely on client‑side checks. |
| NFR‑SEC‑04 | **Rate Limiting** | Authentication endpoints (login, registration, password reset) shall be rate‑limited to **5 requests per minute per IP** to prevent brute force attacks. |
| NFR‑SEC‑05 | **Input Validation** | All user inputs shall be validated server‑side. HTML/script injection shall be sanitized. File uploads shall be scanned and limited to allowed types (PDF, JPEG, PNG, MP4). Max file size: 50 MB. |
| NFR‑SEC‑06 | **Audit Trail** | All Admin actions and all user authentication events shall be logged with timestamp, actor ID, action type, and IP address. Logs shall be immutable and retained for 90 days. |
| NFR‑SEC‑07 | **Data Privacy** | User passwords, refresh tokens, and personally identifiable information (PII) shall be encrypted at rest. PII shall not be exposed in API responses unless explicitly required. |

### 5.4 Scalability

| ID | Requirement | Detail |
|----|-------------|--------|
| NFR‑SCL‑01 | **Horizontal Scaling** | The backend shall be stateless (JWT‑based) to allow horizontal scaling via Azure App Service auto‑scaling (2–10 instances based on CPU > 70 %). |
| NFR‑SCL‑02 | **Database Scaling** | MySQL 8 shall support read replicas for reporting queries when needed. Connection pooling via HikariCP with max pool size configurable. |
| NFR‑SCL‑03 | **Caching** | Frequently accessed data (catalog browse, program details) shall be cached with Spring Cache (in‑memory for MVP; Redis in future). Cache TTL: 5 minutes for catalog, 1 hour for static data. |

### 5.5 Usability & Accessibility

| ID | Requirement | Detail |
|----|-------------|--------|
| NFR‑USA‑01 | **Responsive Design** | The frontend shall be fully responsive and functional on desktop (≥ 1024 px), tablet (≥ 768 px), and mobile (≥ 360 px) viewports. |
| NFR‑USA‑02 | **Accessibility** | The frontend shall meet **WCAG 2.1 Level AA** standards: proper semantic HTML, keyboard navigation, sufficient color contrast (≥ 4.5:1 for text), and ARIA labels on interactive elements. |
| NFR‑USA‑03 | **Browser Support** | The platform shall support the **two latest major versions** of Chrome, Firefox, Safari, and Edge. |
| NFR‑USA‑04 | **Onboarding** | First‑time learners shall see a guided walkthrough (tooltip sequence) on the learning view explaining navigation, progress tracking, and assessments. |

### 5.6 Maintainability

| ID | Requirement | Detail |
|----|-------------|--------|
| NFR‑MNT‑01 | **API Documentation** | All REST APIs shall be documented via Swagger/OpenAPI 3.0 with request/response schemas, example values, and authentication requirements. |
| NFR‑MNT‑02 | **Code Quality** | Test coverage shall be ≥ **80 %** for backend service layer and ≥ **60 %** for React components (unit test). Linting (ESLint + Checkstyle) shall pass before merge. |
| NFR‑MNT‑03 | **Logging** | The application shall log at different levels: ERROR (system failures), WARN (unexpected but handled), INFO (key business events), DEBUG (development only, disabled in production). Log format: JSON for machine parsing. |
| NFR‑MNT‑04 | **Monitoring** | The system shall expose health check endpoints (`/actuator/health`, `/actuator/info`) for Azure App Service monitoring. Application Insights shall track request rates, failure rates, and dependency call times. |

### 5.7 Compliance

| ID | Requirement | Detail |
|----|-------------|--------|
| NFR‑CMP‑01 | **Data Protection** | The system shall comply with applicable data protection regulations. If EU users are present, GDPR requirements apply (right to access, right to deletion, data portability). |
| NFR‑CMP‑02 | **Data Retention** | User data shall be retained for the duration of the account plus 90 days after deactivation. Audit logs retained for 90 days. Deleted accounts shall have PII anonymized within 30 days. |

---

## 6. Out of Scope Features

The following features are **explicitly excluded** from the initial release (MVP). They may be considered for future releases.

| # | Feature | Rationale for Exclusion |
|---|---------|-------------------------|
| OOS‑01 | **Live / Virtual Classroom** (real‑time video conferencing, screen sharing, whiteboarding) | Requires real‑time infrastructure (WebRTC, media servers) that adds significant complexity. Learners may use external tools (Zoom, Google Meet) for live sessions. |
| OOS‑02 | **E‑Commerce / Payment Gateway** (paid programs, subscriptions, checkout flow) | MVP focuses on free programs. Payment integration (Stripe, Razorpay, etc.) will be added in v2. |
| OOS‑03 | **Mobile Native App** (iOS / Android) | The responsive web app covers mobile browsers. Dedicated native apps require separate development effort, planned for v2. |
| OOS‑04 | **AI‑Powered Recommendations** (personalized program suggestions based on learning history) | Valuable but non‑essential for launch. Can be added as a smart feature layer in a later iteration. |
| OOS‑05 | **Gamification** (leaderboards, badges, streaks, experience points) | Adds motivational value but not critical for core learning workflow. Post‑MVP feature. |
| OOS‑06 | **SCORM / xAPI / LTI Integration** (learning interoperability standards) | Enterprise LMS integration is a future requirement. MVP assumes standalone usage. |
| OOS‑07 | **Multi‑Language / i18n** (support for languages other than the default) | MVP ships in English only. Internationalization infrastructure (i18n framework, translations) scoped for future. |
| OOS‑08 | **Social Features** (discussion forums, peer reviews, Q&A, comments on lessons) | Requires moderation tools and adds community management overhead. MVP provides instructor‑only feedback. |
| OOS‑09 | **Advanced Content Authoring** (drag‑and‑drop lesson builder, interactive simulations, branching scenarios) | MVP supports basic rich text, video embeds, and file uploads. Advanced authoring tools are future scope. |
| OOS‑10 | **SAML / SSO Integration** (enterprise single sign‑on with identity providers) | MVP uses email + password and JWT. SSO integration is an enterprise feature for post‑launch. |
| OOS‑11 | **Scheduled / Batch Programs** (cohort‑based programs with fixed start/end dates) | MVP supports self‑paced, always‑open programs only. Cohort‑based scheduling requires timeline features, planned for v2. |
| OOS‑12 | **API Rate Limiting Dashboard** (admin UI for configuring rate limits) | Rate limits are configured at infrastructure level (Azure / API Gateway) in initial release. Admin UI for this is future scope. |

---

> **Next Document:** `03_ARCHITECTURE.md` — Detailed system architecture, C4 diagrams, API contracts, database schemas, and design patterns.
