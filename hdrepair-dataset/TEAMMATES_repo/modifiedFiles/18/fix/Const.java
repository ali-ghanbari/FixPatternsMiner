package teammates.common.util;

import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * Stores constants that are widely used across classes. 
 * this class contains several nested classes, each containing a specific
 * category of constants.
 */

public class Const {
    
    public static class SystemParams{

        public static final String ENCODING = "UTF8";
        public static final int NUMBER_OF_HOURS_BEFORE_CLOSING_ALERT = 24;
        
        /** This is the limit after which TEAMMATES will send error message */
        public static final int MAX_PROFILE_PIC_SIZE = 5000000;
        /** This is the limit given to Blobstore API, beyond which an ugly error page is shown */
        public static final long MAX_PROFILE_PIC_LIMIT_FOR_BLOBSTOREAPI = 11000000;
        
        /** e.g. "2014-04-01 11:59 PM UTC" */
        public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd h:mm a Z";
        /** Number to trim the Google ID when displaying to the user*/
        public static final int USER_ID_MAX_DISPLAY_LENGTH = 23;
        /** Maximum number to do Batching puts/delete documents from a index in the appengine  */
        public static final int MAX_NUM_OF_INPUT_FOR_APP_ENGINE_BATCH = 200;
        /* Field sizes and error messages for invalid fields can be found 
         * in the FieldValidator class.
         */
        public static final String ADMIN_TIME_ZONE = "Asia/Singapore";
        
        public static final String EMAIL_TASK_QUEUE = "configure-and-prepare-email-queue";
        public static final String SUBMISSION_TASK_QUEUE = "submission-queue";
        public static final String EVAL_SUBMISSION_ADJUSTMENT_TASK_QUEUE =
                                "eval-submission-adjust-queue";
        
        public static final String FEEDBACK_SUBMISSION_ADJUSTMENT_TASK_QUEUE = 
                                "feedback-submission-adjust-queue";
        
        public static final String EVAL_PUBLISH_EMAIL_TASK_QUEUE = "evaluation-publish-email-queue";
        public static final String EVAL_REMIND_EMAIL_TASK_QUEUE = "evaluation-remind-email-queue";
        public static final String SEND_EMAIL_TASK_QUEUE = "send-email-queue";
        
        public static final String QUEUE_XML_PATH = "src/main/webapp/WEB-INF/queue.xml";
        public static final String DEFAULT_PROFILE_PICTURE_PATH = "/images/profile_picture_default.png";
        
        public static final List<String> PAGES_ACCESSIBLE_WITHOUT_GOOGLE_LOGIN = Arrays.asList(
            ActionURIs.STUDENT_COURSE_JOIN,
            ActionURIs.STUDENT_COURSE_JOIN_NEW,
            ActionURIs.STUDENT_FEEDBACK_RESULTS_PAGE,
            ActionURIs.STUDENT_FEEDBACK_SUBMISSION_EDIT_PAGE,
            ActionURIs.STUDENT_FEEDBACK_SUBMISSION_EDIT_SAVE,
            ActionURIs.STUDENT_FEEDBACK_QUESTION_SUBMISSION_EDIT_PAGE,
            ActionURIs.STUDENT_FEEDBACK_QUESTION_SUBMISSION_EDIT_SAVE
        );
        
        public static final List<String> PAGES_ACCESSIBLE_WITHOUT_REGISTRATION = Arrays.asList(
            ActionURIs.STUDENT_COURSE_JOIN_AUTHENTICATED,
            ActionURIs.STUDENT_HOME_PAGE,
            ActionURIs.INSTRUCTOR_COURSE_JOIN,
            ActionURIs.INSTRUCTOR_COURSE_JOIN_AUTHENTICATED
        );
        
        public static final List<String> LEGACY_PAGES_WITH_REDUCED_SECURITY = Arrays.asList(
                ActionURIs.STUDENT_COURSE_JOIN
        );
    }

    /* Text displayed to the user when the mouse hover over certain elements in
     * the UI.
     */
    public class Tooltips{
    
        public static final String COURSE_ENROLL = "Enroll student into the course";
        public static final String COURSE_ENROLL_SAMPLE_SPREADSHEET = "Download a sample team data spreadsheet";
        public static final String COURSE_DETAILS = "View, edit and send invitation emails to the students in the course";
        public static final String COURSE_EDIT = "Edit Course information and instructor list";
        public static final String COURSE_DELETE = "Delete the course and its corresponding students and sessions";
        public static final String COURSE_ARCHIVE = "Archive the course so that it will not be shown in the home page any more (you can still access it from the 'Courses' tab)";
        public static final String COURSE_ADD_EVALUATION = "Add a feedback session for the course";
        public static final String CLAIMED = "This is the students own estimation of his/her contributions";
        public static final String PERCEIVED = "This is the average of what other team members think this student contributed";
        public static final String PERCEIVED_CLAIMED = "Difference between claimed and perceived contribution points";
    
        public static final String COURSE_INSTRUCTOR_EDIT = "Edit instructor details";
        public static final String COURSE_INSTRUCTOR_DELETE = "Delete the instructor from the course";
        public static final String COURSE_INSTRUCTOR_REMIND = "Send invitation email to the instructor";
        
        public static final String COURSE_STUDENT_DETAILS = "View the details of the student";
        public static final String COURSE_STUDENT_EDIT = "Use this to edit the details of this student. <br>To edit multiple students"
                + " in one go, you can use the enroll page: <br>Simply enroll students using the updated data and existing data will be updated accordingly";
        public static final String COURSE_STUDENT_REMIND = "Email an invitation to the student requesting him/her to join the course using his/her "
                + "Google Account. Note: Students can use TEAMMATES without ‘joining’, but a joined student can access extra features e.g. set up a user profile";
        public static final String COURSE_STUDENT_DELETE = "Delete the student and the corresponding submissions from the course";
        public static final String COURSE_STUDENT_RECORDS = "View all data about this student";
        public static final String COURSE_STUDENT_COMMENT = "Give a comment for this student, his/her team/section";
        
        public static final String COURSE_REMIND = "Email an invitation to all students yet to join requesting them to join the course"
                + " using their Google Accounts. Note: Students can use TEAMMATES without ‘joining’, but a joined student can access"
                + " extra features e.g. set up a user profile";
        public static final String COURSE_DELETE_ALL_STUDENTS = "Delete all students in this course";
    
        public static final String EVALUATION_STATUS_AWAITING = "The evaluation is created but has not yet started";
        public static final String EVALUATION_STATUS_OPEN = "The evaluation has started and students can submit feedback until the closing time";
        public static final String EVALUATION_STATUS_CLOSED = "The evaluation has finished but the results have not been made available to the students";
        public static final String EVALUATION_STATUS_PUBLISHED = "The evaluation has finished and the results have been made available to students";
    
        public static final String EVALUATION_RESULTS = "View the current results of the evaluation";
        public static final String EVALUATION_EDIT = "Edit evaluation details";
        public static final String EVALUATION_PREVIEW = "Preview evaluation session";
        public static final String EVALUATION_REMIND = "Send e-mails to remind students who have not submitted their evaluations to do so";
        public static final String EVALUATION_DELETE = "Delete the evaluation";
        public static final String EVALUATION_PUBLISH = "Publish evaluation results for students to view";
        public static final String EVALUATION_UNPUBLISH = "Make results not visible to students";
        public static final String EVALUATION_SUBMIT = "Start evaluation";
        public static final String EVALUATION_EDIT_SUBMISSION = "Edit submitted evaluation";
        
        public static final String EVALUATION_DIFF = "Perceived Contribution - Claimed Contribution";
        public static final String EVALUATION_RESPONSE_RATE = "Number of students submitted / Class size";
        public static final String EVALUATION_POINTS_RECEIVED = "The list of points that this student received from others";
    
        public static final String EVALUATION_INPUT_COURSE = "Please select the course for which the evaluation is to be created.";
        public static final String EVALUATION_INPUT_START = "Please enter the start date for the evaluation.";
        public static final String EVALUATION_INPUT_NAME = "Enter the name of the evaluation e.g. Mid-term Evaluation 1.";
        public static final String EVALUATION_INPUT_DEADLINE = "Please enter deadline for the evaluation.";
        public static final String EVALUATION_INPUT_COMMENTSSTATUS = "Enable this if you want students to give anonymous feedback to team members.<br />"
                + "You can moderate those peer feedback before publishing it to the team.";
        public static final String EVALUATION_INPUT_TIMEZONE = "You should not need to change this as your timezone is auto-detected. <br /><br />However, note that daylight saving is not taken into account i.e. if you are in UTC -8:00 and there is "
                + "daylight saving, you should choose UTC -7:00 and its corresponding timings.";
        public static final String EVALUATION_INPUT_GRACEPERIOD = "Please select the amount of time that the system will continue accepting <br />"
                + "submissions after the specified deadline.";
        public static final String EVALUATION_INPUT_INSTRUCTIONS = "Please enter instructions for your students, e.g. Avoid comments which are too critical.";
    
        public static final String EVALUATION_SUBMISSION_VIEW_REVIEWER = "View feedback from the student for his team<br />This opens in a new window";
        public static final String EVALUATION_SUBMISSION_VIEW_REVIEWEE = "View feedback from the team for the student<br />This opens in a new window";
        public static final String EVALUATION_SUBMISSION_INSTRUCTOR_EDIT = "Edit feedback from the student for his team<br />This opens in a new window";
    
        public static final String EVALUATION_SUBMISSION_NOT_AVAILABLE = "Not Available: There is no data for this or the data is not enough";
        public static final String EVALUATION_SUBMISSION_NOT_SURE = "Not sure about the contribution";
        
        public static final String INSTRUCTOR_DISPLAYED_TO_STUDENT = "If this is unselected, the instructor will be completely invisible to students." 
                + " E.g. to give access to a colleague for ‘auditing’ your course";
        
        public static final String INSTRUCTOR_DISPLAYED_AS = "Specify the role of this instructor in this course as shown to the students";
    
        public static final String STUDENT_COURSE_PROFILE = "Your profile in this course";
        public static final String STUDENT_COURSE_DETAILS = "View and edit information regarding your team";
    
        public static final String STUDENT_EVALUATION_STATUS_PENDING = "The evaluation is yet to be completed by you";
        public static final String STUDENT_EVALUATION_STATUS_SUBMITTED = "You have submitted your feedback for this evaluation";
        public static final String STUDENT_EVALUATION_STATUS_CLOSED = "The evaluation has finished but the instructor has not published the results yet";
        public static final String STUDENT_EVALUATION_STATUS_PUBLISHED = "The evaluation has finished and you can check the results";
        public static final String STUDENT_EVALUATION_STATUS_ERROR = "There were some errors in retrieving this evaluation.";

        public static final String STUDENT_FEEDBACK_SESSION_STATUS_AWAITING = "The session is not open for submission at this time. It is expected to open later.";
        public static final String STUDENT_FEEDBACK_SESSION_STATUS_PENDING = "The feedback session is yet to be completed by you.";
        public static final String STUDENT_FEEDBACK_SESSION_STATUS_SUBMITTED = "You have submitted your feedback for this session.";
        public static final String STUDENT_FEEDBACK_SESSION_STATUS_CLOSED = "<br />The session is now closed for submissions.";
        public static final String STUDENT_FEEDBACK_SESSION_STATUS_PUBLISHED = "<br />The responses for the session can now be viewed.";
    
        public static final String FEEDBACK_SESSION_COURSE = "Please select the course for which the feedback session is to be created.";
        public static final String FEEDBACK_SESSION_INPUT_NAME = "Enter the name of the feedback session e.g. Feedback Session 1.";
        public static final String FEEDBACK_SESSION_STARTDATE = "Please select the date and time for which users can start submitting responses for the feedback session.";
        public static final String FEEDBACK_SESSION_ENDDATE = "Please select the date and time after which the feedback session will no longer accept submissions from users.";
        public static final String FEEDBACK_SESSION_VISIBLEDATE = "Select this option to enter in a custom date and time for which the feedback session will become visible.<br />"
                + "Note that you can make a session visible before it is open for submissions so that users can preview the questions.";
        public static final String FEEDBACK_SESSION_PUBLISHDATE = "Select this option to enter in a custom date and time for which</br>"
                + "the responses for this feedback session will become visible.";
        public static final String FEEDBACK_SESSION_SESSIONVISIBLELABEL = "Please select when you want the questions for the feedback session to be visible to users who need to participate. Note that users cannot submit their responses until the submissions opening time set below.";
        public static final String FEEDBACK_SESSION_SESSIONVISIBLECUSTOM = "Select this option to use a custom time for when the session will become visible to users. ";
        public static final String FEEDBACK_SESSION_SESSIONVISIBLEATOPEN = "Select this option to have the feedback session become visible "
                + "when it is open for submissions (as selected above).";
        public static final String FEEDBACK_SESSION_SESSIONVISIBLENEVER = "Select this option if you want the feedback session to never be visible. "
                + "Use this option if you want to use this as a private feedback session.";
        public static final String FEEDBACK_SESSION_RESULTSVISIBLELABEL = "Please select when the responses for the feedback session will be visible to the designated recipients."
                + "<br />You can select the response visibility for each type of user and question later.";
        public static final String FEEDBACK_SESSION_RESULTSVISIBLECUSTOM = "Select this option to use a custom time for when the responses of the feedback session<br />"
                + "will be visible to the designated recipients.";
        public static final String FEEDBACK_SESSION_RESULTSVISIBLEATVISIBLE = "Select this option to have the feedback responses be immediately visible<br />"
                + "when the session becomes visible to users.";
        public static final String FEEDBACK_SESSION_RESULTSVISIBLELATER = "Select this option if you intend to manually publish the session later on.";
        public static final String FEEDBACK_SESSION_RESULTSVISIBLENEVER = "Select this option if you intend never to publish the responses.";
        public static final String FEEDBACK_SESSION_SENDJOINEMAIL = "If the student has not joined the course yet, an email containing the link to join the course will automatically be sent on session opening time.";
        public static final String FEEDBACK_SESSION_SENDOPENEMAIL = "Select this option to automatically send an email to students to notify them when the session is open for submission.";
        public static final String FEEDBACK_SESSION_SENDCLOSINGEMAIL = "Select this option to automatically send an email to students to remind them to submit 24 hours before the end of the session.";
        public static final String FEEDBACK_SESSION_SENDPUBLISHEDEMAIL = "Select this option to automatically send an email to students to notify them when the session results is published.";
        public static final String FEEDBACK_SESSION_INSTRUCTIONS = "Enter instructions for this feedback session. e.g. Avoid comments which are too critical.<br /> It will be displayed at the top of the page when users respond to the session.";
        public static final String FEEDBACK_SESSION_STATUS_PRIVATE = "This is a private session. Nobody can see it but you.";
        public static final String FEEDBACK_SESSION_STATUS_VISIBLE = ", is visible";
        public static final String FEEDBACK_SESSION_STATUS_AWAITING = ", and is waiting to open";
        public static final String FEEDBACK_SESSION_STATUS_OPEN = ", and is open for submissions";
        public static final String FEEDBACK_SESSION_STATUS_CLOSED = ", and has ended";
        public static final String FEEDBACK_SESSION_STATUS_PUBLISHED = ".<br />The responses for this session are visible";
        
        public static final String FEEDBACK_SESSION_RESULTS = "View the submitted responses for this feedback session";
        public static final String FEEDBACK_SESSION_EDIT = "Edit feedback session details";
        public static final String FEEDBACK_SESSION_REMIND = "Send e-mails to remind students and instructors who have not submitted their feedbacks to do so";
        public static final String FEEDBACK_SESSION_DELETE = "Delete the feedback session";
        public static final String FEEDBACK_SESSION_SUBMIT = "Start submitting feedback";
        public static final String FEEDBACK_SESSION_PUBLISH = "Make session responses available for viewing";
        public static final String FEEDBACK_SESSION_UNPUBLISH = "Make responses no longer visible";
        public static final String FEEDBACK_SESSION_AUTOPUBLISH = "This session will be published automatically at the specified time";
        public static final String FEEDBACK_SESSION_AWAITING = "This session is not yet opened";
        public static final String FEEDBACK_SESSION_EDIT_SUBMITTED_RESPONSE = "Edit submitted feedback";
        public static final String FEEDBACK_SESSION_VIEW_SUBMITTED_RESPONSE = "View submitted feedback";
        public static final String FEEDBACK_SESSION_RECIPIENT = "Who the feedback is about";
        public static final String FEEDBACK_SESSION_GIVER = "Who will give feedback";
        public static final String FEEDBACK_SESSION_NOT_CREATOR_EDIT = "Only the creator can edit this session";
        public static final String FEEDBACK_SESSION_NOT_CREATOR_PUBLISH = "Only the creator can publish this session";
        public static final String FEEDBACK_SESSION_NOT_CREATOR_UNPUBLISH = "Only the creator can unpublish this session";
        public static final String FEEDBACK_SESSION_NOT_CREATOR_DELETE = "Only the creator can delete this session";
        
        public static final String FEEDBACK_SESSION_EDIT_SAVE = "You can save your responses at any time and come back later to continue.";

        public static final String FEEDBACK_PREVIEW_ASSTUDENT = "View how this session would look like to a student who is submitting feedback.<br>Preview is unavailable if the course has yet to have any student enrolled.";
        public static final String FEEDBACK_PREVIEW_ASINSTRUCTOR = "View how this session would look like to an instructor who is submitting feedback.";
        
        public static final String FEEDBACK_QUESTION_INPUT_INSTRUCTIONS = "Please enter the question for users to give feedback about. e.g. What is the biggest weakness of the presented product?";
        public static final String FEEDBACK_QUESTION_EDIT = "Edit this question";
        public static final String FEEDBACK_QUESTION_GETLINK = "Get a submission link to this particular question. Useful if you want students to answer individual questions separately or at different points in time.";
        public static final String FEEDBACK_QUESTION_DELETE = "Delete this question";
        public static final String FEEDBACK_QUESTION_VISBILITY = "Here you can select how each question's response will be visible to the different types of users in your course.";
        public static final String FEEDBACK_QUESTION_NUMSCALE_MAX = "Maximum acceptable response value";
        public static final String FEEDBACK_QUESTION_NUMSCALE_STEP = "Value to be increased/decreased each step";
        public static final String FEEDBACK_QUESTION_NUMSCALE_MIN = "Minimum acceptable response value";
        
        public static final String FEEDBACK_RESPONSE_VISIBILITY_INFO = "Here you can see the visibility of your feedback to the various users" +
                "<br />in the course once the results are published.";
        public static final String FEEDBACK_RESPONSE_SAVE = "You can submit your responses at any time and come back later to continue " +
                "before the session closes.";
        
        public static final String STUDENT_PROFILE_PICTURE = "Upload a profile picture";
        public static final String STUDENT_PROFILE_SHORTNAME = "This is the name you prefer to be called by";
        public static final String STUDENT_PROFILE_EMAIL = "This is a long term contact email";
        public static final String STUDENT_PROFILE_INSTITUTION = "This is the institution that you represent";
        public static final String STUDENT_PROFILE_NATIONALITY = "This is your nationality";
        public static final String STUDENT_PROFILE_MOREINFO = "You may specify miscellaneous info about yourself "
                + "e.g. links to home page, online CV, portfolio etc.";
        
        public static final String VISIBILITY_OPTIONS_RECIPIENT = "Control what feedback recipient(s) can view";
        public static final String VISIBILITY_OPTIONS_GIVER_TEAM_MEMBERS = "Control what team members of feedback giver can view";
        public static final String VISIBILITY_OPTIONS_RECIPIENT_TEAM_MEMBERS = "Control what team members of feedback recipients can view";
        public static final String VISIBILITY_OPTIONS_OTHER_STUDENTS = "Control what other students can view";
        public static final String VISIBILITY_OPTIONS_INSTRUCTORS = "Control what instructors can view";
        
        public static final String SHOW_EMAILS = "Show emails of all currently listed students";
        
        public static final String COMMENT_ADD = "Add comment";
        public static final String COMMENT_EDIT = "Edit this comment";
        public static final String COMMENT_DELETE = "Delete this comment";
        
        public static final String SEARCH_STUDENT = "Search for student's name";
    }
    
    public class FeedbackQuestionTypeNames{
        public static final String TEXT = "Essay question";
        public static final String MCQ = "Multiple-choice (single answer) question";
        public static final String MSQ = "Multiple-choice (multiple answers) question";
        public static final String NUMSCALE = "Numerical-scale question";
        public static final String CONSTSUM_OPTION = "Distribute points (among options) question";
        public static final String CONSTSUM_RECIPIENT = "Distribute points (among recipients) question";
        public static final String CONTRIB = "Team contribution question";
    }
    
    public class InstructorPermissionRoleNames {
        public final static String INSTRUCTOR_PERMISSION_ROLE_COOWNER = "Co-owner";
        public final static String INSTRUCTOR_PERMISSION_ROLE_MANAGER = "Manager";
        public final static String INSTRUCTOR_PERMISSION_ROLE_OBSERVER = "Observer";
        public final static String INSTRUCTOR_PERMISSION_ROLE_TUTOR = "Tutor";
        public final static String INSTRUCTOR_PERMISSION_ROLE_CUSTOM = "Custom";
    }
    
    public class GenderTypes{
        public static final String MALE = "male";
        public static final String FEMALE = "female";
        public static final String OTHER = "other";
    }

    public class ParamsNames{
        
        public static final String BLOB_KEY = "blob-key";
        
        public static final String COPIED_FEEDBACK_SESSION_NAME = "copiedfsname";
        public static final String COPIED_COURSE_ID = "copiedcourseid";

        public static final String COURSE_ID = "courseid";
        public static final String COURSE_NAME = "coursename";
        public static final String INSTRUCTOR_SHORT_NAME = "instructorshortname";
        public static final String INSTRUCTOR_ID = "instructorid";
        public static final String INSTRUCTOR_EMAIL = "instructoremail";
        public static final String INSTRUCTOR_INSTITUTION = "instructorinstitution";
        public static final String INSTRUCTOR_NAME = "instructorname";
        public static final String STUDENTS_ENROLLMENT_INFO = "enrollstudents";
        public static final String INSTRUCTOR_IMPORT_SAMPLE = "importsample";
        
        public static final String INSTRUCTOR_IS_DISPLAYED_TO_STUDENT = "instructorisdisplayed";
        public static final String INSTRUCTOR_DISPLAY_NAME = "instructordisplayname";
        public static final String INSTRUCTOR_ROLE_NAME = "instructorrole";
        public static final String INSTRUCTOR_SECTION = "section";
        public static final String INSTRUCTOR_SECTION_GROUP = "sectiongroup";
        
        public static final String INSTRUCTOR_PERMISSION_MODIFY_COURSE = "canmodifycourse";
        public static final String INSTRUCTOR_PERMISSION_MODIFY_INSTRUCTOR = "canmodifyinstructor";
        public static final String INSTRUCTOR_PERMISSION_MODIFY_SESSION = "canmodifysession";
        public static final String INSTRUCTOR_PERMISSION_MODIFY_STUDENT = "canmodifystudent";
        
        public static final String INSTRUCTOR_PERMISSION_VIEW_STUDENT_IN_SECTIONS = "canviewstudentinsection";
        public static final String INSTRUCTOR_PERMISSION_VIEW_COMMENT_IN_SECTIONS = "canviewcommentinsection";
        public static final String INSTRUCTOR_PERMISSION_GIVE_COMMENT_IN_SECTIONS = "cangivecommentinsection";
        public static final String INSTRUCTOR_PERMISSION_MODIFY_COMMENT_IN_SECTIONS = "canmodifycommentinsection";
        
        public static final String INSTRUCTOR_PERMISSION_VIEW_SESSION_IN_SECTIONS = "canviewsessioninsection";
        public static final String INSTRUCTOR_PERMISSION_SUBMIT_SESSION_IN_SECTIONS = "cansubmitsessioninsection";
        public static final String INSTRUCTOR_PERMISSION_MODIFY_SESSION_COMMENT_IN_SECTIONS = "canmodifysessioncommentinsection";
        
        public static final String SHOW_COMMENT_BOX = "addComment";
        
        public static final String COURSE_SORTING_CRITERIA = "sortby";
        
        public static final String COURSE_ARCHIVE_STATUS = "archive";
        
        public static final String ADMIN_SEARCH_KEY = "searchkey";
        public static final String ADMIN_SEARCH_BUTTON_HIT = "searchbuttonhit";
    
        public static final String EVALUATION_NAME = "evaluationname";
    
        public static final String EVALUATION_START = "start";
        public static final String EVALUATION_STARTTIME = "starttime";
        public static final String EVALUATION_DEADLINE = "deadline";
        public static final String EVALUATION_DEADLINETIME = "deadlinetime";
        public static final String EVALUATION_TIMEZONE = "timezone";
    
        public static final String EVALUATION_COMMENTSENABLED = "commentsstatus";
        public static final String EVALUATION_GRACEPERIOD = "graceperiod";
        public static final String EVALUATION_INSTRUCTIONS = "instr";
        public static final String EVALUATION_NUMBEROFCOMPLETEDEVALUATIONS = "numberofevaluations";
        public static final String EVALUATION_NUMBEROFEVALUATIONS = "numberofcompletedevaluations";
        public static final String EVALUATION_PUBLISHED = "published";
        public static final String EVALUATION_TYPE = "evaluationtype";
        
        public static final String FEEDBACK_SESSION_NAME = "fsname";
        public static final String FEEDBACK_SESSION_CREATOR = "fscreator";
        public static final String FEEDBACK_SESSION_CREATEDATE = "createdate";
        public static final String FEEDBACK_SESSION_CREATETIME = "createtime";
        public static final String FEEDBACK_SESSION_STARTDATE = "startdate";
        public static final String FEEDBACK_SESSION_STARTTIME = "starttime";
        public static final String FEEDBACK_SESSION_STARTHOUR = "starthour";
        public static final String FEEDBACK_SESSION_STARTMINUTE = "startminute";
        public static final String FEEDBACK_SESSION_ENDDATE = "enddate";
        public static final String FEEDBACK_SESSION_ENDTIME = "endtime";
        public static final String FEEDBACK_SESSION_ENDHOUR = "endhour";
        public static final String FEEDBACK_SESSION_ENDMINUTE = "endminute";
        public static final String FEEDBACK_SESSION_VISIBLEDATE = "visibledate";
        public static final String FEEDBACK_SESSION_VISIBLETIME = "visibletime";
        public static final String FEEDBACK_SESSION_PUBLISHDATE = "publishdate";
        public static final String FEEDBACK_SESSION_PUBLISHTIME = "publishtime";
        public static final String FEEDBACK_SESSION_TIMEZONE = "timezone";
        public static final String FEEDBACK_SESSION_GRACEPERIOD = "graceperiod";
        public static final String FEEDBACK_SESSION_TYPE = "fstype";
        public static final String FEEDBACK_SESSION_OPENEMAILSENT = "fsopenemailsent";
        public static final String FEEDBACK_SESSION_PUBLISHEDEMAILSENT = "fspublishedemailsent";
        public static final String FEEDBACK_SESSION_SESSIONVISIBLEBUTTON = "sessionVisibleFromButton";
        public static final String FEEDBACK_SESSION_RESULTSVISIBLEBUTTON = "resultsVisibleFromButton";
        public static final String FEEDBACK_SESSION_SENDREMINDEREMAIL = "sendreminderemail";
        public static final String FEEDBACK_SESSION_INSTRUCTIONS = "instructions";

        public static final String FEEDBACK_QUESTION_ID = "questionid";
        public static final String FEEDBACK_QUESTION_NUMBER = "questionnum";
        public static final String FEEDBACK_QUESTION_TEXT = "questiontext";
        public static final String FEEDBACK_QUESTION_TYPE = "questiontype";
        public static final String FEEDBACK_QUESTION_NUMBEROFCHOICECREATED = "noofchoicecreated";
        public static final String FEEDBACK_QUESTION_MCQCHOICE = "mcqOption";
        public static final String FEEDBACK_QUESTION_MSQCHOICE = "msqOption";
        public static final String FEEDBACK_QUESTION_CONSTSUMOPTION = "constSumOption";
        public static final String FEEDBACK_QUESTION_CONSTSUMTORECIPIENTS = "constSumToRecipients";
        public static final String FEEDBACK_QUESTION_CONSTSUMNUMOPTION = "constSumNumOption";
        public static final String FEEDBACK_QUESTION_CONSTSUMPOINTSPEROPTION = "constSumPointsPerOption";
        public static final String FEEDBACK_QUESTION_CONSTSUMPOINTS = "constSumPoints";
        public static final String FEEDBACK_QUESTION_GENERATEDOPTIONS = "generatedOptions";
        public static final String FEEDBACK_QUESTION_GIVERTYPE = "givertype";
        public static final String FEEDBACK_QUESTION_RECIPIENTTYPE = "recipienttype";
        public static final String FEEDBACK_QUESTION_NUMBEROFENTITIES = "numofrecipients";
        public static final String FEEDBACK_QUESTION_NUMBEROFENTITIESTYPE = "numofrecipientstype";
        public static final String FEEDBACK_QUESTION_EDITTEXT = "questionedittext";
        public static final String FEEDBACK_QUESTION_EDITTYPE = "questionedittype";
        public static final String FEEDBACK_QUESTION_SAVECHANGESTEXT = "questionsavechangestext";
        public static final String FEEDBACK_QUESTION_GETLINK = "questiongetlink";
        public static final String FEEDBACK_QUESTION_SHOWRESPONSESTO = "showresponsesto";
        public static final String FEEDBACK_QUESTION_SHOWGIVERTO = "showgiverto";
        public static final String FEEDBACK_QUESTION_SHOWRECIPIENTTO = "showrecipientto";
        public static final String FEEDBACK_QUESTION_RESPONSETOTAL = "questionresponsetotal";
        public static final String FEEDBACK_QUESTION_NUMSCALE_MIN = "numscalemin";
        public static final String FEEDBACK_QUESTION_NUMSCALE_MAX = "numscalemax";
        public static final String FEEDBACK_QUESTION_NUMSCALE_STEP = "numscalestep";
    
        public static final String FEEDBACK_RESPONSE_ID = "responseid";
        public static final String FEEDBACK_RESPONSE_RECIPIENT = "responserecipient";
        public static final String FEEDBACK_RESPONSE_TEXT = "responsetext";
        
        public static final String FEEDBACK_RESPONSE_COMMENT_ID = "responsecommentid";
        public static final String FEEDBACK_RESPONSE_COMMENT_TEXT = "responsecommenttext";
        
        public static final String FEEDBACK_RESULTS_UPLOADDOWNLOADBUTTON = "fruploaddownloadbtn";
        public static final String FEEDBACK_RESULTS_SORTTYPE = "frsorttype";
        public static final String FEEDBACK_RESULTS_GROUPBYTEAM = "frgroupbyteam";
        public static final String FEEDBACK_RESULTS_GROUPBYSECTION = "frgroupbysection";
        public static final String FEEDBACK_RESULTS_SHOWSTATS = "frshowstats";
        public static final String FEEDBACK_RESULTS_NEED_AJAX = "frneedajax";
        public static final String FEEDBACK_RESULTS_MAIN_INDEX ="frmainindex";

        public static final String PREVIEWAS = "previewas";
        
        public static final String STUDENT_ID = "googleid";
        
        public static final String REGKEY = "key";
        public static final String STUDENT_EMAIL = "studentemail";
        public static final String FROM_COMMENTS_PAGE = "commentpage";
        public static final String FROM_COURSE_DETAILS_PAGE = "coursedetailspage";
        public static final String FROM_STUDENT_DETAILS_PAGE = "studentdetailspage";
        public static final String NEW_STUDENT_EMAIL = "newstudentemail";
        
        public static final String STUDENT_SHORT_NAME = "studentshortname";
        public static final String STUDENT_PROFILE_EMAIL = "studentprofileemail";
        public static final String STUDENT_PROFILE_INSTITUTION = "studentprofileinstitute";
        public static final String STUDENT_NATIONALITY = "studentnationality";
        public static final String STUDENT_GENDER = "studentgender";
        public static final String STUDENT_PROFILE_MOREINFO = "studentprofilemoreinfo"; 
        public static final String STUDENT_PROFILE_PHOTO = "studentprofilephoto";
        public static final String STUDENT_PROFILE_PHOTOEDIT = "editphoto";
    
        public static final String STUDENT_NAME = "studentname";
        public static final String RECIPIENT_TYPE = "recipienttype";
        public static final String RECIPIENTS = "recipients";
        public static final String RESPONSE_COMMENTS_SHOWCOMMENTSTO = "showresponsecommentsto";
        public static final String RESPONSE_COMMENTS_SHOWGIVERTO = "showresponsegiverto";
        public static final String COMMENTS_SHOWCOMMENTSTO = "showcommentsto";
        public static final String COMMENTS_SHOWGIVERTO = "showgiverto";
        public static final String COMMENTS_SHOWRECIPIENTTO = "showrecipientto";
        public static final String FROM_EMAIL = "fromemail";
        public static final String TO_EMAIL = "toemail";
        public static final String SECTION_NAME = "sectionname";
        public static final String TEAM_NAME = "teamname";
        public static final String POINTS = "points";
        public static final String JUSTIFICATION = "justification";
        public static final String COMMENTS = "comments";
        public static final String TEAMMATES = "teammates";
    
        public static final String STATUS_MESSAGE = "message";
        public static final String ERROR = "error";
        public static final String NEXT_URL = "next";
        public static final String USER_ID = "user";
        public static final String HINT = "hint";
    
        public static final String LOGIN_ADMIN = "admin";
        public static final String LOGIN_INSTRUCTOR = "instructor";
        public static final String LOGIN_STUDENT = "student";
        
        //Email parameters
        public static final String EMAIL_RECEIVER = "user";
        public static final String EMAIL_COURSE = "course";
        public static final String EMAIL_EVAL = "evaluation";
        public static final String EMAIL_FEEDBACK = "feedback";
        public static final String EMAIL_TYPE = "type";
        public static final String EMAIL_IS_STUDENT = "isStudent";
        
        public static final String EMAIL_CONTENT = "content";
        public static final String EMAIL_SENDER = "sender";
        public static final String EMAIL_SUBJECT = "subject";
        public static final String EMAIL_REPLY_TO_ADDRESS = "reply";
        
        public static final String COMMENT_EDITTYPE = "commentedittype";
        public static final String COMMENT_ID = "commentid";
        public static final String COMMENT_TEXT = "commenttext";
        
        //Submission parameters for Task Queue
        public static final String SUBMISSION_COURSE = "course";
        public static final String SUBMISSION_EVAL = "evaluation";
        
        public static final String ENROLLMENT_DETAILS = "enrollmentdetails";
        
        public static final String SEARCH_KEY ="searchkey";
        public static final String DISPLAY_ARCHIVE ="displayarchive";
        
        //Parameters for checking persistence of data during Eventual Consistency
        public static final String CHECK_PERSISTENCE_COURSE = "persistencecourse";
        public static final String CHECK_PERSISTENCE_EVALUATION = "persistenceevalaution";
        
        public static final String PROFILE_PICTURE_LEFTX = "cropboxleftx";
        public static final String PROFILE_PICTURE_TOPY = "cropboxtopy";
        public static final String PROFILE_PICTURE_RIGHTX = "cropboxrightx";
        public static final String PROFILE_PICTURE_BOTTOMY = "cropboxbottomy";
        public static final String PROFILE_PICTURE_HEIGHT = "pictureheight";
        public static final String PROFILE_PICTURE_WIDTH = "picturewidth";
        
        public static final String SEARCH_STUDENTS = "searchstudents";
        public static final String SEARCH_COMMENTS_FOR_STUDENTS = "searchcommentforstudents";
        public static final String SEARCH_COMMENTS_FOR_RESPONSES = "searchcommentforresponses";
    }
    
    public class SearchIndex {
        public static final String COMMENT = "comment";
        public static final String FEEDBACK_RESPONSE_COMMENT = "feedbackresponsecomment";
        public static final String STUDENT = "student";
    }
    
    public class SearchDocumentField {
        public static final String ATTRIBUTE = "attribute";
        public static final String COMMENT_ATTRIBUTE = "commentAttibute";
        public static final String STUDENT_ATTRIBUTE = "studentAttribute";
        public static final String COMMENT_GIVER_NAME = "commentGiverName";
        public static final String COMMENT_GIVER_EMAIL = "commentGiverEmail";
        public static final String COMMENT_RECIPIENT_NAME = "commentRecipientName";
        public static final String FEEDBACK_RESPONSE_COMMENT_ATTRIBUTE = "frCommentAttibute";
        public static final String FEEDBACK_RESPONSE_COMMENT_GIVER_NAME = "frCommentGiverName";
        public static final String FEEDBACK_RESPONSE_COMMENT_GIVER_EMAIL = "frCommentGiverEmail";
        public static final String FEEDBACK_RESPONSE_ATTRIBUTE = "feedbackResponseAttibute";
        public static final String FEEDBACK_RESPONSE_GIVER_NAME = "feedbackResponseGiverName";
        public static final String FEEDBACK_RESPONSE_RECEIVER_NAME = "feedbackResponseReceiverName";
        public static final String FEEDBACK_QUESTION_ATTRIBUTE = "feedbackQuestionAttibute";
        public static final String FEEDBACK_SESSION_ATTRIBUTE = "feedbackSessionAttibute";
        public static final String SEARCHABLE_TEXT = "searchableText";
        public static final String CREATED_DATE = "createdDate";
        public static final String COURSE_ID = "courseId";
        public static final String GIVER_EMAIL = "giverEmail";
        public static final String GIVER_SECTION = "giverSection";
        public static final String RECIPIENT_EMAIL = "recipientEmail";
        public static final String RECIPIENT_SECTION = "recipientSection";
        public static final String IS_VISIBLE_TO_INSTRUCTOR = "isVisibleToInstructor";
        public static final String IS_VISIBLE_TO_RECEIVER = "isVisibleToReceiver";
        public static final String IS_VISIBLE_TO_GIVER = "isVisibleToGiver";
    }

    public class ActionURIs{
        
        /* _PAGE/Page in the Action URI name means 'show page' */
    
        public static final String LOGIN = "/login";
    
        public static final String INSTRUCTOR_HOME_PAGE = "/page/instructorHomePage";
        public static final String INSTRUCTOR_COURSES_PAGE = "/page/instructorCoursesPage";
        public static final String INSTRUCTOR_COURSE_ADD = "/page/instructorCourseAdd";
        public static final String INSTRUCTOR_COURSE_DELETE = "/page/instructorCourseDelete";
        public static final String INSTRUCTOR_COURSE_ARCHIVE = "/page/instructorCourseArchive";
        public static final String INSTRUCTOR_COURSE_DETAILS_PAGE = "/page/instructorCourseDetailsPage";
        public static final String INSTRUCTOR_COURSE_EDIT_PAGE = "/page/instructorCourseEditPage";
        public static final String INSTRUCTOR_COURSE_EDIT_SAVE = "/page/instructorCourseEditSave";
        public static final String INSTRUCTOR_COURSE_STUDENT_DETAILS_PAGE = "/page/instructorCourseStudentDetailsPage";
        public static final String INSTRUCTOR_COURSE_STUDENT_DETAILS_EDIT = "/page/instructorCourseStudentDetailsEdit";
        public static final String INSTRUCTOR_COURSE_STUDENT_DETAILS_EDIT_SAVE = "/page/instructorCourseStudentDetailsEditSave";
        public static final String INSTRUCTOR_COURSE_STUDENT_DELETE = "/page/instructorCourseStudentDelete";
        public static final String INSTRUCTOR_COURSE_STUDENT_LIST_DOWNLOAD = "/page/instructorCourseStudentListDownload";
        public static final String INSTRUCTOR_COURSE_ENROLL_PAGE = "/page/instructorCourseEnrollPage";
        public static final String INSTRUCTOR_COURSE_ENROLL_SAVE = "/page/instructorCourseEnrollSave";
        public static final String INSTRUCTOR_COURSE_REMIND = "/page/instructorCourseRemind";
        public static final String INSTRUCTOR_COURSE_INSTRUCTOR_ADD = "/page/instructorCourseInstructorAdd";
        public static final String INSTRUCTOR_COURSE_INSTRUCTOR_EDIT_SAVE = "/page/instructorCourseInstructorEditSave";
        public static final String INSTRUCTOR_COURSE_INSTRUCTOR_DELETE = "/page/instructorCourseInstructorDelete";
        public static final String INSTRUCTOR_COURSE_JOIN = "/page/instructorCourseJoin";
        public static final String INSTRUCTOR_COURSE_JOIN_AUTHENTICATED = "/page/instructorCourseJoinAuthenticated";
        public static final String INSTRUCTOR_EVALS_PAGE = "/page/instructorEvalsPage";
        public static final String INSTRUCTOR_EVAL_ADD = "/page/instructorEvalAdd";
        public static final String INSTRUCTOR_EVAL_DELETE = "/page/instructorEvalDelete";
        public static final String INSTRUCTOR_EVAL_EDIT_PAGE = "/page/instructorEvalEditPage";
        public static final String INSTRUCTOR_EVAL_EDIT_SAVE = "/page/instructorEvalEditSave";
        public static final String INSTRUCTOR_EVAL_PREVIEW = "/page/instructorEvalPreview";
        public static final String INSTRUCTOR_EVAL_RESULTS_PAGE = "/page/instructorEvalResultsPage";
        public static final String INSTRUCTOR_EVAL_STATS_PAGE = "/page/instructorEvalStatsPage";
        public static final String INSTRUCTOR_EVAL_SUBMISSION_PAGE = "/page/instructorEvalSubmissionPage";
        public static final String INSTRUCTOR_EVAL_SUBMISSION_EDIT = "/page/instructorEvalSubmissionEdit";
        public static final String INSTRUCTOR_EVAL_SUBMISSION_EDIT_SAVE = "/page/instructorEvalSubmissionEditSave";
        public static final String INSTRUCTOR_EVAL_REMIND = "/page/instructorEvalRemind";
        public static final String INSTRUCTOR_EVAL_PUBLISH = "/page/instructorEvalPublish";
        public static final String INSTRUCTOR_EVAL_UNPUBLISH = "/page/instructorEvalUnpublish";
        public static final String INSTRUCTOR_EVAL_RESULTS_DOWNLOAD = "/page/instructorEvalResultsDownload";
        public static final String INSTRUCTOR_SEARCH_PAGE = "/page/instructorSearchPage";
        public static final String INSTRUCTOR_STUDENT_LIST_PAGE = "/page/instructorStudentListPage";
        public static final String INSTRUCTOR_STUDENT_LIST_AJAX_PAGE = "/page/instructorStudentListAjaxPage"; 

        public static final String INSTRUCTOR_STUDENT_RECORDS_PAGE = "/page/instructorStudentRecordsPage";
        public static final String INSTRUCTOR_STUDENT_COMMENT_ADD = "/page/instructorStudentCommentAdd";
        public static final String INSTRUCTOR_STUDENT_COMMENT_EDIT = "/page/instructorStudentCommentEdit";
        public static final String INSTRUCTOR_STUDENT_COMMENT_CLEAR_PENDING = "/page/instructorStudentCommentClearPending";
        
        public static final String INSTRUCTOR_COMMENTS_PAGE = "/page/instructorCommentsPage";
        
        public static final String INSTRUCTOR_FEEDBACKS_PAGE = "/page/instructorFeedbacksPage";
        public static final String INSTRUCTOR_FEEDBACK_ADD = "/page/instructorFeedbackAdd";
        public static final String INSTRUCTOR_FEEDBACK_COPY = "/page/instructorFeedbackCopy";
        public static final String INSTRUCTOR_FEEDBACK_DELETE = "/page/instructorFeedbackDelete";
        public static final String INSTRUCTOR_FEEDBACK_REMIND = "/page/instructorFeedbackRemind";
        public static final String INSTRUCTOR_FEEDBACK_PUBLISH = "/page/instructorFeedbackPublish";
        public static final String INSTRUCTOR_FEEDBACK_UNPUBLISH = "/page/instructorFeedbackUnpublish";
        public static final String INSTRUCTOR_FEEDBACK_EDIT_PAGE = "/page/instructorFeedbackEditPage";
        public static final String INSTRUCTOR_FEEDBACK_EDIT_SAVE = "/page/instructorFeedbackEditSave";
        public static final String INSTRUCTOR_FEEDBACK_RESULTS_PAGE = "/page/instructorFeedbackResultsPage";
        public static final String INSTRUCTOR_FEEDBACK_RESULTS_DOWNLOAD = "/page/instructorFeedbackResultsDownload";
        public static final String INSTRUCTOR_FEEDBACK_PREVIEW_ASSTUDENT = "/page/instructorFeedbackPreviewAsStudent";
        public static final String INSTRUCTOR_FEEDBACK_PREVIEW_ASINSTRUCTOR = "/page/instructorFeedbackPreviewAsInstructor";
        
        public static final String INSTRUCTOR_FEEDBACK_RESULTS_AJAX_RESPONSE_RATE = "/page/instructorFeedbackResultsAjaxResponseRate";

        public static final String INSTRUCTOR_FEEDBACK_QUESTION_ADD = "/page/instructorFeedbackQuestionAdd";
        public static final String INSTRUCTOR_FEEDBACK_QUESTION_COPY = "/page/instructorFeedbackQuestionCopy";
        public static final String INSTRUCTOR_FEEDBACK_QUESTION_EDIT = "/page/instructorFeedbackQuestionEdit";
        public static final String INSTRUCTOR_FEEDBACK_QUESTION_VISIBILITY_MESSAGE = "/page/instructorFeedbackQuestionvisibilityMessage";
        
        public static final String INSTRUCTOR_FEEDBACK_RESPONSE_COMMENT_ADD = "/page/instructorFeedbackResponseCommentAdd";
        public static final String INSTRUCTOR_FEEDBACK_RESPONSE_COMMENT_EDIT = "/page/instructorFeedbackResponseCommentEdit";
        public static final String INSTRUCTOR_FEEDBACK_RESPONSE_COMMENT_DELETE = "/page/instructorFeedbackResponseCommentDelete";
                
        public static final String INSTRUCTOR_FEEDBACK_STATS_PAGE = "/page/feedbackSessionStatsPage";
        
        public static final String INSTRUCTOR_FEEDBACK_SUBMISSION_EDIT_PAGE = "/page/instructorFeedbackSubmissionEditPage";
        public static final String INSTRUCTOR_FEEDBACK_SUBMISSION_EDIT_SAVE = "/page/instructorFeedbackSubmissionEditSave";
        public static final String INSTRUCTOR_FEEDBACK_QUESTION_SUBMISSION_EDIT_PAGE = "/page/instructorFeedbackQuestionSubmissionEditPage";
        public static final String INSTRUCTOR_FEEDBACK_QUESTION_SUBMISSION_EDIT_SAVE = "/page/instructorFeedbackQuestionSubmissionEditSave";
        
        public static final String STUDENT_HOME_PAGE = "/page/studentHomePage";
        public static final String STUDENT_COURSE_JOIN = "/page/studentCourseJoin";
        public static final String STUDENT_COURSE_JOIN_NEW = "/page/studentCourseJoinAuthentication";
        public static final String STUDENT_COURSE_JOIN_AUTHENTICATED = "/page/studentCourseJoinAuthenticated";
        public static final String STUDENT_COMMENTS_PAGE = "/page/studentCommentsPage";
        public static final String STUDENT_COURSE_DETAILS_PAGE = "/page/studentCourseDetailsPage";
        public static final String STUDENT_EVAL_SUBMISSION_EDIT_PAGE = "/page/studentEvalSubmissionEditPage";
        public static final String STUDENT_EVAL_SUBMISSION_EDIT_SAVE = "/page/studentEvalSubmissionEditSave";
        public static final String STUDENT_EVAL_RESULTS_PAGE = "/page/studentEvalResultsPage";
        
        public static final String STUDENT_FEEDBACK_SUBMISSION_EDIT_PAGE = "/page/studentFeedbackSubmissionEditPage";
        public static final String STUDENT_FEEDBACK_SUBMISSION_EDIT_SAVE = "/page/studentFeedbackSubmissionEditSave";
        public static final String STUDENT_FEEDBACK_QUESTION_SUBMISSION_EDIT_PAGE = "/page/studentFeedbackQuestionSubmissionEditPage";
        public static final String STUDENT_FEEDBACK_QUESTION_SUBMISSION_EDIT_SAVE = "/page/studentFeedbackQuestionSubmissionEditSave";
        public static final String STUDENT_FEEDBACK_RESULTS_PAGE = "/page/studentFeedbackResultsPage";
        public static final String STUDENT_PROFILE_PAGE = "/page/studentProfilePage";
        public static final String STUDENT_PROFILE_EDIT_SAVE = "/page/studentProfileEditSave";
        public static final String STUDENT_PROFILE_PICTURE = "/page/studentProfilePic";
        public static final String STUDENT_PROFILE_PICTURE_UPLOAD = "/page/studentProfilePictureUpload";
        public static final String STUDENT_PROFILE_PICTURE_EDIT = "/page/studentProfilePictureEdit";
        public static final String STUDENT_PROFILE_CREATEUPLOADFORMURL = "/page/studentProfileCreateFormUrl";
        
        public static final String ADMIN_HOME_PAGE = "/admin/adminHomePage";
        public static final String ADMIN_INSTRUCTORACCOUNT_ADD = "/admin/adminInstructorAccountAdd";
        public static final String ADMIN_ACCOUNT_MANAGEMENT_PAGE = "/admin/adminAccountManagementPage";
        public static final String ADMIN_ACCOUNT_DETAILS_PAGE = "/admin/adminAccountDetailsPage";
        public static final String ADMIN_ACCOUNT_DELETE = "/admin/adminAccountDelete";
        public static final String ADMIN_EXCEPTION_TEST = "/admin/adminExceptionTest";
        public static final String ADMIN_ACTIVITY_LOG_PAGE = "/admin/adminActivityLogPage";
        public static final String ADMIN_SESSIONS_PAGE = "/admin/adminSessionsPage";
        public static final String ADMIN_SEARCH_PAGE = "/admin/adminSearchPage";
        
        public static final String AUTOMATED_EVAL_OPENING_REMINDERS = "/evaluationopeningreminders";
        public static final String AUTOMATED_EVAL_CLOSING_REMINDERS = "/evaluationclosingreminders";        
        public static final String AUTOMATED_FEEDBACK_OPENING_REMINDERS = "/feedbackSessionOpeningReminders";
        public static final String AUTOMATED_FEEDBACK_CLOSING_REMINDERS = "/feedbackSessionClosingReminders";
        public static final String AUTOMATED_FEEDBACK_PUBLISHED_REMINDERS = "/feedbackSessionPublishedReminders";
        public static final String AUTOMATED_COMPILE_LOGS = "/compileLogs";
        
        public static final String BACKDOOR = "/backdoor";
        
        //Task Queue Worker Servlets URI
        public static final String EMAIL_WORKER = "/emailWorker";
        public static final String SUBMISSION_WORKER = "/submissionWorker";
        public static final String EVAL_SUBMISSION_ADJUSTMENT_WORKER = 
                                    "/evalSubmissionAdjustmentWorker";
        public static final String FEEDBACK_SUBMISSION_ADJUSTMENT_WORKER = 
                                    "/feedbackSubmissionAdjustmentWorker";
        public static final String EVAL_PUBLISH_EMAIL_WORKER = "/evalPublishEmailWorker";
        public static final String EVAL_REMIND_EMAIL_WORKER = "/evalRemindEmailWorker";
        public static final String SEND_EMAIL_WORKER = "/sendEmailWorker";
    }

    public class ViewURIs{
        
        /* We omit adding the 'page' prefix to views because all of them are "pages" */
    
        public static final String INSTRUCTOR_HOME = "/jsp/instructorHome.jsp"; 
        public static final String INSTRUCTOR_COMMENTS = "/jsp/instructorComments.jsp";
        public static final String INSTRUCTOR_COURSES = "/jsp/instructorCourses.jsp"; 
        public static final String INSTRUCTOR_COURSE_EDIT = "/jsp/instructorCourseEdit.jsp"; 
        public static final String INSTRUCTOR_COURSE_DETAILS = "/jsp/instructorCourseDetails.jsp"; 
        public static final String INSTRUCTOR_COURSE_STUDENT_DETAILS = "/jsp/instructorCourseStudentDetails.jsp"; 
        public static final String INSTRUCTOR_COURSE_STUDENT_EDIT = "/jsp/instructorCourseStudentEdit.jsp"; 
        public static final String INSTRUCTOR_COURSE_ENROLL = "/jsp/instructorCourseEnroll.jsp"; 
        public static final String INSTRUCTOR_COURSE_ENROLL_RESULT = "/jsp/instructorCourseEnrollResult.jsp";
        public static final String INSTRUCTOR_COURSE_JOIN_CONFIRMATION = "/jsp/instructorCourseJoinConfirmation.jsp";
        public static final String INSTRUCTOR_EVALS = "/jsp/instructorEvals.jsp"; 
        public static final String INSTRUCTOR_EVAL_EDIT = "/jsp/instructorEvalEdit.jsp"; 
        public static final String INSTRUCTOR_EVAL_RESULTS = "/jsp/instructorEvalResults.jsp"; 
        public static final String INSTRUCTOR_EVAL_STATS = "/jsp/instructorEvalStats.jsp";
        public static final String INSTRUCTOR_EVAL_SUBMISSION = "/jsp/instructorEvalSubmission.jsp"; 
        public static final String INSTRUCTOR_EVAL_SUBMISSION_EDIT = "/jsp/instructorEvalSubmissionEdit.jsp"; 
        public static final String INSTRUCTOR_FEEDBACKS = "/jsp/instructorFeedbacks.jsp";
        public static final String INSTRUCTOR_FEEDBACK_EDIT = "/jsp/instructorFeedbackEdit.jsp";
        public static final String INSTRUCTOR_FEEDBACK_RESULTS_TOP = "/jsp/instructorFeedbackResultsTop.jsp";
        public static final String INSTRUCTOR_FEEDBACK_RESULTS_BY_GIVER_RECIPIENT_QUESTION = "/jsp/instructorFeedbackResultsByGiverRecipientQuestion.jsp";
        public static final String INSTRUCTOR_FEEDBACK_RESULTS_BY_RECIPIENT_GIVER_QUESTION = "/jsp/instructorFeedbackResultsByRecipientGiverQuestion.jsp"; 
        public static final String INSTRUCTOR_FEEDBACK_RESULTS_BY_GIVER_QUESTION_RECIPIENT = "/jsp/instructorFeedbackResultsByGiverQuestionRecipient.jsp";
        public static final String INSTRUCTOR_FEEDBACK_RESULTS_BY_RECIPIENT_QUESTION_GIVER = "/jsp/instructorFeedbackResultsByRecipientQuestionGiver.jsp";
        public static final String INSTRUCTOR_FEEDBACK_RESULTS_BY_QUESTION = "/jsp/instructorFeedbackResultsByQuestion.jsp"; 
        public static final String INSTRUCTOR_FEEDBACK_SUBMISSION_EDIT = "/jsp/instructorFeedbackSubmissionEdit.jsp"; 
        public static final String INSTRUCTOR_FEEDBACK_QUESTION_SUBMISSION_EDIT = "/jsp/instructorFeedbackQuestionSubmissionEdit.jsp"; 
        public static final String INSTRUCTOR_FEEDBACK_STATS = "/jsp/instructorFeedbackStats.jsp";
        public static final String INSTRUCTOR_SEARCH = "/jsp/instructorSearch.jsp";
        public static final String INSTRUCTOR_STUDENT_LIST = "/jsp/instructorStudentList.jsp";
        public static final String INSTRUCTOR_STUDENT_RECORDS = "/jsp/instructorStudentRecords.jsp";
        
        public static final String STUDENT_HOME = "/jsp/studentHome.jsp";
        public static final String STUDENT_COURSE_JOIN_CONFIRMATION = "/jsp/studentCourseJoinConfirmation.jsp";
        public static final String STUDENT_COURSE_DETAILS = "/jsp/studentCourseDetails.jsp"; 
        public static final String STUDENT_COMMENTS = "/jsp/studentComments.jsp"; 
        public static final String STUDENT_EVAL_SUBMISSION_EDIT = "/jsp/studentEvalEdit.jsp"; 
        public static final String STUDENT_EVAL_RESULTS = "/jsp/studentEvalResults.jsp"; 
        public static final String STUDENT_FEEDBACK_SUBMISSION_EDIT = "/jsp/studentFeedbackSubmissionEdit.jsp"; 
        public static final String STUDENT_FEEDBACK_QUESTION_SUBMISSION_EDIT = "/jsp/studentFeedbackQuestionSubmissionEdit.jsp"; 
        public static final String STUDENT_FEEDBACK_RESULTS = "/jsp/studentFeedbackResults.jsp";
        public static final String STUDENT_PROFILE_PAGE = "/jsp/studentProfilePage.jsp";
            
        public static final String ADMIN_HOME = "/jsp/adminHome.jsp";
        public static final String ADMIN_ACCOUNT_MANAGEMENT = "/jsp/adminAccountManagement.jsp";
        public static final String ADMIN_SEARCH = "/jsp/adminSearch.jsp";
        public static final String ADMIN_ACTIVITY_LOG = "/jsp/adminActivityLog.jsp";
        public static final String ADMIN_ACCOUNT_DETAILS = "/jsp/adminAccountDetails.jsp";
        public static final String ADMIN_SESSIONS = "/jsp/adminSessions.jsp";
        
        public static final String LOGOUT = "/logout.jsp"; 
        
        
        public static final String SHOW_MESSAGE = "/showMessage.jsp"; 
        public static final String UNAUTHORIZED = "/unauthorized.jsp"; 
        public static final String ERROR_PAGE = "/errorPage.jsp"; 
        public static final String DEADLINE_EXCEEDED_ERROR_PAGE = "/deadlineExceededErrorPage.jsp"; 
        public static final String ENTITY_NOT_FOUND_PAGE = "/entityNotFoundPage.jsp"; 
        public static final String ACTION_NOT_FOUND_PAGE = "/pageNotFound.jsp"; 
        public static final String MAINTENANCE_PAGE = "/maintenance.jsp";
        
        public static final String MASHUP = "/dev/mashup.jsp";
    
        //View fragments
        public static final String INSTRUCTOR_HEADER = "/jsp/instructorHeader.jsp"; 
        public static final String STUDENT_HEADER = "/jsp/studentHeader.jsp"; 
        public static final String ADMIN_HEADER = "/jsp/adminHeader.jsp"; 
        public static final String FOOTER = "/jsp/footer.jsp"; 
        public static final String STATUS_MESSAGE = "/jsp/statusMessage.jsp";
        public static final String STATUS_MESSAGE_WITHOUT_FOCUS = "/jsp/statusMessageWithoutFocusingToStatus.jsp";
        public static final String EVAL_SUBMISSION_EDIT = "/jsp/evalSubmissionEdit.jsp";
        public static final String FEEDBACK_SUBMISSION_EDIT = "/jsp/feedbackSubmissionEdit.jsp";
        public static final String FEEDBACK_QUESTION_SUBMISSION_EDIT = "/jsp/feedbackQuestionSubmissionEdit.jsp"; 
    }

    /* These are status messages that may be shown to the user */
    public class StatusMessages{
        public static final String LOADING = "<img src=\"/images/ajax-loader.gif\" /><br />";
        public static final String STUDENT_FIRST_TIME = "<div style=\"text-align:left;\">Welcome stranger :-) "
                + "<br/><br/>It seems you are not a registered user of TEAMMATES. To use TEAMMATES, a course instructor has to add you to a course first. "
                + "After that, TEAMMATES will send you an email containing the link to 'join' that course. "
                + "<br/><br/>Not a stranger to TEAMMATES? Could log in before, but not any more? These are the possible reasons:"
                + "<br/>1. You used a different Google account to access TEAMMATES in the past. "
                + "In that case, you need to use the same Google account to access TEAMMATES again. Logout and re-login using the other Google account. "
                + "If you don't remember which Google account you used previously, email us from the same email account to which you receive TEAMMATES emails."
                + "<br/>2. You changed the primary email from a non-Gmail address to a Gmail address recently. " 
                + "In that case, <a href='http://www.comp.nus.edu.sg/%7Eteams/contact.html'>email us</a> so that we can reconfigure your account to use the new Gmail address."
                + "<br/>3. You joined this course just a few seconds ago and your data may be still in the process of propagating through our servers. "
                + "In that case, please click on the <a href=" + ActionURIs.STUDENT_HOME_PAGE + ">Home</a> link above in a few minutes. "
                + "</div>";
        public static final String INVALID_EMAIL = "\"%s\" is not acceptable to TEAMMATES as an email because it is not in the correct format."
                + " An email address contains some text followed by one '@' sign followed by some more text. It cannot be longer than 45 characters. It cannot be empty and it cannot have spaces.";

        public static final String COURSE_ADDED = "The course has been added.. Click <a href=\"${courseEnrollLink}\">here</a> to add students to the course "
                + "or click <a href=\"${courseEditLink}\">here</a> to add other instructors.<br>"
                + "If you don't see the course in the list below, please refresh the page after a few moments.";
        public static final String COURSE_EXISTS = "A course by the same ID already exists in the system, possibly created by another user. Please choose a different course ID";
        public static final String COURSE_EDITED = "The course has been edited.";
        public static final String COURSE_ARCHIVED = "The course %s has been archived. It will not appear in the home page any more.";
        //TODO: Let undo process to be in the Course page for now. Should implement to be able to undo the archiving from the home page later.
        public static final String COURSE_ARCHIVED_FROM_HOMEPAGE = COURSE_ARCHIVED + " You can access archived courses from the 'Courses' tab.<br>Go there to undo the archiving and bring the course back to the home page.";
        public static final String COURSE_UNARCHIVED = "The course %s has been unarchived.";
        public static final String COURSE_DELETED = "The course %s has been deleted.";
        public static final String COURSE_EMPTY = "You have not created any courses yet. Use the form above to create a course.";
        public static final String COURSE_EMPTY_IN_EVALUATION = "You have not created any courses yet. Go <a href=\""
                + ActionURIs.INSTRUCTOR_COURSES_PAGE + "${user}\">here</a> to create one.";
        public static final String COURSE_REMINDER_SENT_TO = "An email has been sent to ";
        public static final String COURSE_REMINDERS_SENT = "Emails have been sent to unregistered students.";
        
        public static final String COURSE_ENROLL_STUDENTS_ERROR = "Errors on %d student(s):";
        public static final String COURSE_ENROLL_STUDENTS_ADDED = "%d student(s) added:";
        public static final String COURSE_ENROLL_STUDENTS_MODIFIED = "%d student(s) modified:";
        public static final String COURSE_ENROLL_STUDENTS_UNMODIFIED = "%d student(s) updated with no changes:";
        public static final String COURSE_ENROLL_STUDENTS_NOT_IN_LIST = "%d student(s) remain unmodified:";
        public static final String COURSE_ENROLL_STUDENTS_UNKNOWN = "%d student(s) with unknown enrolment status:";

        public static final String TEAM_INVALID_SECTION_EDIT = "The team \"%s\" is in multiple sections. The team ID should be unique across the entire course and a team cannot be spread across multiple sections.<br>";
        public static final String SECTION_QUOTA_EXCEED = "You are trying enroll more than 100 students in section \"%s\". To avoid performance problems, please do not enroll more than 100 students in a single section.<br>";

        public static final String COURSE_INSTRUCTOR_ADDED = "The instructor %s has been added successfully. "
                + "An email containing how to 'join' this course will be sent to %s in a few minutes.";
        public static final String COURSE_INSTRUCTOR_EXISTS = "An instructor with the same email address already exists in the course.";
        public static final String COURSE_INSTRUCTOR_EDITED = "The changes to the instructor has been updated.";
        public static final String COURSE_INSTRUCTOR_DELETED = "The instructor has been deleted from the course.";
        public static final String COURSE_INSTRUCTOR_DELETE_NOT_ALLOWED = "The instructor you are trying to delete is the last instructor in the course. "
                + "Deleting the last instructor from the course is not allowed.";
        
        public static final String JOIN_COURSE_KEY_BELONGS_TO_DIFFERENT_USER = "The join link used belongs to a different user whose Google ID is "
                + "%s (only part of the Google ID is shown to protect privacy). "
                + "If that Google ID is owned by you, please logout and re-login "
                + "using that Google account. If it doesn’t belong to you, please "
                + "<a href=\"mailto:teammates@comp.nus.edu.sg?"
                + "body=Your name:%%0AYour course:%%0AYour university:\">"
                + "contact us</a> so that we can investigate.";
        public static final String JOIN_COURSE_GOOGLE_ID_BELONGS_TO_DIFFERENT_USER = "The Google ID %s belongs to an existing user in the course."
                + "Please login again using a different Google account, and try to join the course again.";
        
        public static final String STUDENT_EDITED = "The student has been edited successfully";
        public static final String STUDENT_DELETED = "The student has been removed from the course";
        public static final String STUDENT_EMAIL_CONFLIT = "Trying to update to an email that is already used by: ";
        public static final String STUDENT_PROFILE_EDITED = "Your profile has been edited successfully";
        public static final String STUDENT_PROFILE_PICTURE_SAVED = "Your profile picture has been saved successfully";
        public static final String STUDENT_PROFILE_PIC_TOO_LARGE = "The uploaded profile picture was too large. "
                + "Please try again with a smaller picture.";
        public static final String STUDENT_PROFILE_PIC_SERVICE_DOWN = "We were unable to upload your picture at this time. "
                + "Please try again after some time";
        
        public static final String EVALUATION_ADDED = "The evaluation has been added. If you don't see that evaluation in the list below, please refresh the page after a few moments.";
        public static final String EVALUATION_DELETED = "The evaluation has been deleted.";
        public static final String EVALUATION_EDITED = "The evaluation has been edited.";
        public static final String EVALUATION_INFORMEDSTUDENTSOFCHANGES = "E-mails have been sent out to inform the students of the changes to the evaluation.";
        public static final String EVALUATION_PUBLISHED = "The evaluation has been published.";
        public static final String EVALUATION_UNPUBLISHED = "The evaluation has been unpublished.";
        public static final String EVALUATION_REMINDERSSENT = "Reminder e-mails have been sent out to those students.";
        public static final String EVALUATION_RESULTSEDITED = "The particular evaluation results have been edited.";
        public static final String EVALUATION_EMPTY = "You have not created any sessions yet. Use the form above to create a session.";
        public static final String EVALUATION_NOT_OPEN = "This evaluation is not open at this time. You are not allowed to edit your submission.";
        public static final String EVALUATION_EXISTS = "An evaluation by this name already exists under this course";
        public static final String EVALUATION_REQUEST_EXPIRED = "<div style=\"text-align:left;\">"
                + "The request we received seems to be incomplete. "
                + "This may be due to reloading a page from a submission activity you did earlier. "
                + "If you typed your input in the previous page, you can try to click the 'Back' button of your Browser to take a copy of what you typed. "
                + "If you wish to submit now, please re-login and try to submit again. "
                + "If your second attempt to submit did not work either, please email us at "
                + "<a href='mailto:teammates@comp.nus.edu.sg?subject=Evaluation Request Expired&body=System Message: Evaluation submission failed possibly due to missing parameters.'>teammates@comp.nus.edu.sg</a>."
                + "</div>";
        
        public static final String FEEDBACK_SESSION_ADDED = "The feedback session has been added. Click the \"Add New Question\" button below to begin adding questions for the feedback session.";
        public static final String FEEDBACK_SESSION_ADD_DB_INCONSISTENCY = "If you do not see existing feedback sessions in the list below, please refresh the page after a few moments";
        public static final String FEEDBACK_SESSION_COPIED = "The feedback session has been copied. Please modify settings/questions as necessary.";
        public static final String FEEDBACK_SESSION_EDITED = "The feedback session has been updated.";
        public static final String FEEDBACK_SESSION_DELETED = "The feedback session has been deleted.";
        public static final String FEEDBACK_SESSION_PUBLISHED = "The feedback session has been published.";
        public static final String FEEDBACK_SESSION_UNPUBLISHED = "The feedback session has been unpublished.";
        public static final String FEEDBACK_SESSION_REMINDERSSENT = "Reminder e-mails have been sent out to those students and instructors.";
        public static final String FEEDBACK_SESSION_EXISTS = "A feedback session by this name already exists under this course";        
        public static final String FEEDBACK_SESSION_EMPTY = "You have not created any sessions yet. Use the form above to create a session.";
    
        public static final String FEEDBACK_QUESTION_ADDED = "The question has been added to this feedback session.";
        public static final String FEEDBACK_QUESTION_EDITED = "The changes to the question has been updated.";
        public static final String FEEDBACK_QUESTION_DELETED = "The question has been deleted.";
        public static final String FEEDBACK_QUESTION_EXISTS = "The requested question has already been created.";
        public static final String FEEDBACK_QUESTION_EMPTY = "You have not created any questions for this feedback session yet. Click the button below to add a feedback question.";
        public static final String FEEDBACK_QUESTION_NUMBEROFENTITIESINVALID = "Please enter the maximum number of recipients each respondants should give feedback to.";
        public static final String FEEDBACK_QUESTION_TEXTINVALID = "Please enter a valid question. The question text cannot be empty.";
        
        public static final String FEEDBACK_RESPONSES_SAVED = "All responses submitted succesfully!";
        
        public static final String FEEDBACK_RESPONSE_COMMENT_EMPTY = "Comment cannot be empty";
        public static final String FEEDBACK_RESPONSE_COMMENT_ADDED = "Your comment has been saved successfully";
        public static final String FEEDBACK_RESPONSE_COMMENT_EDITED = "Your changes has been saved successfully";
        public static final String FEEDBACK_RESPONSE_COMMENT_DELETED = "Your comment has been deleted successfully";
        
        public static final String FEEDBACK_SUBMISSIONS_NOT_OPEN = "You can view the questions and any submitted responses for this feedback session but cannot submit new responses as the session is not currently open for submission.";
        public static final String FEEDBACK_SUBMISSION_EXCEEDED_DEADLINE = "<strong>Submission Failure!</strong> You have exceeded the submission deadline.";
        
        public static final String FEEDBACK_RESULTS_SOMETHINGNEW = "You have received feedback from others. Please see below.";
        public static final String FEEDBACK_RESULTS_NOTHINGNEW = "You have not received any new feedback but you may review your own submissions below.";
        
        public static final String ENROLL_LINE_EMPTY = "Please input at least one student detail.";
        public static final String ENROLL_LINES_PROBLEM_DETAIL_PREFIX = "&bull;";
        public static final String ENROLL_LINES_PROBLEM = "<p><span class=\"bold\">Problem in line : <span class=\"invalidLine\">%s</span></span>" +
                                                        "<br><span class=\"problemDetail\">" + ENROLL_LINES_PROBLEM_DETAIL_PREFIX + " %s</span></p>";
        
        public static final String EVENTUAL_CONSISTENCY_MESSAGE_STUDENT = "You have successfully joined the course %1$s. "
                + "<br>Updating of the course data on our servers is currently in progress and will be completed in a few minutes. "
                + "<br>Please refresh this page in a few minutes to see the course %1$s in the list below.";
        
        public static final String NULL_POST_PARAMETER_MESSAGE = "?message=You have been redirected to this page due to a possible expiry of the previous login."
                + "<br>If you have previously typed some data and wish to retrieve it, you may use the 'Back' button of your Browser to navigate to the"
                + " previous page containing the data you typed in.";
        
        //TODO: these status messages are from used for input validation testing only. Move to test driver side? 
        
        public static final String COURSE_INPUT_FIELDS_EXTRA = "There are too many fields.";
        public static final String COURSE_INPUT_FIELDS_MISSING = "There are missing fields.";
        public static final String COURSE_GOOGLEID_INVALID = "GoogleID should only consist of alphanumerics, fullstops, dashes or underscores.";
        public static final String COURSE_EMAIL_INVALID = "The e-mail address is invalid.";
        public static final String COURSE_INSTRUCTORNAME_INVALID = "Name should only consist of alphanumerics or hyphens, apostrophes, fullstops, "
                + "commas, slashes, round brackets\nand not more than " + FieldValidator.COURSE_INSTRUCTORNAME_MAX_LENGTH + " characters.";
        public static final String COURSE_COURSE_ID_EMPTY = "Course ID cannot be empty.";
        public static final String COURSE_COURSE_NAME_EMPTY = "Course name cannot be empty";
        public static final String COURSE_INSTRUCTOR_LIST_EMPTY = "Instructor list cannot be empty";
        public static final String COURSE_INVALID_ID = "Please use only alphabets, numbers, dots, hyphens, underscores and dollar signs in course ID.";
        public static final String COURSE_STUDENTNAME_INVALID = "Name should only consist of alphanumerics or hyphens, apostrophes, fullstops, "
                + "commas, slashes, round brackets\nand not more than " + FieldValidator.COURSE_STUDENTNAME_MAX_LENGTH + " characters.";
        public static final String COURSE_TEAMNAME_INVALID = "Team name should contain less than " + FieldValidator.COURSE_TEAMNAME_MAX_LENGTH
                + " characters.";
        
        public static final String EVALUATION_NAMEINVALID = "Please use only alphabets, numbers and whitespace in evaluation name.";
        public static final String EVALUATION_NAME_LENGTHINVALID = "Evaluation name should not exceed 38 characters.";
        public static final String EVALUATION_SCHEDULEINVALID = "The evaluation schedule (start/deadline) is not valid.<br />"
                + "The start time should be in the future, and the deadline should be after start time.";
        public static final String FIELDS_EMPTY = "Please fill in all the relevant fields.";
    
        public static final String INSTRUCTOR_STATUS_DELETED = "The Instructor status has been deleted";
        public static final String INSTRUCTOR_ACCOUNT_DELETED = "The Account has been deleted";
        public static final String INSTRUCTOR_REMOVED_FROM_COURSE = "The Instructor has been removed from the Course";
        
        public static final String INSTRUCTOR_COURSE_EMPTY = "There are no students in this course";
        public static final String INSTRUCTOR_NO_COURSE_AND_STUDENTS = "There are no course or students information to be displayed";
        public static final String INSTRUCTOR_NO_STUDENT_RECORDS = "No records were found for this student";
        
        public static final String COMMENT_ADDED = "New comment has been added";
        public static final String COMMENT_EDITED = "Comment edited";
        public static final String COMMENT_DELETED = "Comment deleted";
        public static final String COMMENT_CLEARED = "Notification for all pending comments have been sent to recipients";
        public static final String COMMENT_CLEARED_UNSUCCESSFULLY = "Notification for some pending comments fails to send";
        public static final String COMMENT_DUPLICATE = "An existing comment with the same content is found, comment not added";
        
        public static final String HINT_FOR_NEW_INSTRUCTOR = "New to TEAMMATES? You may wish to have a look at our "
                + "<a href='/instructorHelp.html#gs' target='_blank'>Getting Started Guide</a>.<br>A <a href='https://youtube.googleapis.com/v/wCxBOUEiD6Q&hd=1&autoplay=1&rel=0' target='_blank'>video tour</a>"
                + " is also available in our <a href='/index.html' target='_blank'>home page</a>.";
        
        // Messages that are templates only
        /** Template String. Parameters: Student's name, Evaluation name, Course ID */
        public static final String STUDENT_COURSE_JOIN_SUCCESSFUL = "You have been successfully added to the course (%s).";
        public static final String INSTRUCTOR_EVALUATION_SUBMISSION_RECEIVED = "You have edited %s's submission for evaluation %s in course %s successfully.<br />"
                + "The change will not be reflected here until you <span class='color_red bold'>REFRESH</span> the page.";
        /** Template String. Parameters: Evaluation name, Course ID */
        public static final String STUDENT_EVALUATION_SUBMISSION_RECEIVED = "Your submission for %s in course %s has been saved successfully";
        public static final String STUDENT_PROFILE_NOT_A_PICTURE = "The file that you have uploaded is not a picture. "
                + "Please upload a picture (usually it ends with .jpg or .png)";
        public static final String STUDENT_PROFILE_NO_PICTURE_GIVEN = "Please specify a file to be uploaded.";
        public static final String STUDENT_NOT_FOUND_FOR_RECORDS = "The student you tried to view records for does not exist.";
        public static final String STUDENT_NOT_FOUND_FOR_COURSE_DETAILS = "The student you tried to view details for does not exist.";
        public static final String STUDENT_PROFILE_PICTURE_EDIT_FAILED = "The photo that was edited did not belong to the user. "
                + "Please upload another picture to begin editing";
        public static final String STUDENT_NOT_JOINED_YET_FOR_RECORDS = "This student has not joined the course yet."; 
        public static final String STUDENT_PROFILE_UNACCESSIBLE_TO_INSTRUCTOR = "You do not have access "
                + "to view this student's profile";
        
        public static final String UNREGISTERED_STUDENT = "You may submit feedback and view results without logging in. "
                + "To access other features you need <a href='%s' class='link'>to login using a google account</a> "
                + "(recommended).";
    }

    /* These indicate status of an operation, but they are not shown to the user */
    public class StatusCodes{
    
        // Backdoor responses
        public static final String BACKDOOR_STATUS_SUCCESS = "[BACKDOOR_STATUS_SUCCESS]";
        public static final String BACKDOOR_STATUS_FAILURE = "[BACKDOOR_STATUS_FAILURE]";
    
        // General Error codes
        public static final String ACTIVATED_BEFORE_START = "ERRORCODE_ACTIVATED_BEFORE_START";
        public static final String ALREADY_JOINED = "ERRORCODE_ALREADY_JOINED";
        public static final String EMPTY_STRING = "ERRORCODE_EMPTY_STRING";
        public static final String END_BEFORE_START = "ERRORCODE_END_BEFORE_START";
        public static final String NULL_PARAMETER = "ERRORCODE_NULL_PARAMETER";
        public static final String INCORRECTLY_FORMATTED_STRING = "ERRORCODE_INCORRECTLY_FORMATTED_STRING";
        public static final String INVALID_CHARS = "ERRORCODE_IVALID_CHARS";
        public static final String INVALID_EMAIL = "ERRORCODE_INVALID_EMAIL";
        public static final String INVALID_KEY = "ERRORCODE_INVALID_KEY";
        public static final String KEY_BELONGS_TO_DIFFERENT_USER = "ERRORCODE_KEY_BELONGS_TO_DIFFERENT_USER";
        public static final String LEADING_OR_TRAILING_SPACES = "ERRORCODE_LEADING_OR_TRAILING_SPACES";
        public static final String PUBLISHED_BEFORE_CLOSING = "ERRORCODE_PUBLISHED_BEFORE_CLOSING";
        public static final String STRING_TOO_LONG = "ERRORCODE_STRING_TOO_LONG";
        public static final String UNPUBLISHED_BEFORE_PUBLISHING = "ERRORCODE_UNPUBLISHED_BEFORE_PUBLISHING";
        
        // Error message used across DB level
        public static final String DBLEVEL_NULL_INPUT = "Supplied parameter was null\n";
    
        // Task Queue Response Success code
        public static final int TASK_QUEUE_RESPONSE_OK = 200;
        
        // POST parameter null message
        public static final String NULL_POST_PARAMETER = "The %s POST parameter is null\n";
    }

    /* This section holds constants that are defined as constants primarily 
     * because they are repeated in many places.
     */
    @SuppressWarnings("unused")
    private void _______repeated_phrases___________________________________(){}
    
    public static final String EOL = System.getProperty("line.separator");
    
    public static final String USER_NOBODY_TEXT = "-";
    public static final String USER_UNKNOWN_TEXT = "Unknown user";
    public static final String TEAM_OF_EMAIL_OWNER = "'s Team";
    
    public static final String INSTRUCTOR_EVALUATION_STATUS_AWAITING = "Awaiting";
    public static final String INSTRUCTOR_EVALUATION_STATUS_OPEN = "Open";
    public static final String INSTRUCTOR_EVALUATION_STATUS_CLOSED = "Closed";
    public static final String INSTRUCTOR_EVALUATION_STATUS_PUBLISHED = "Published";

    public static final String STUDENT_EVALUATION_STATUS_PENDING = "Pending";
    public static final String STUDENT_EVALUATION_STATUS_SUBMITTED = "Submitted";
    public static final String STUDENT_EVALUATION_STATUS_CLOSED = "Closed";
    public static final String STUDENT_EVALUATION_STATUS_PUBLISHED = "Published";
    public static final String STUDENT_EVALUATION_STATUS_ERROR = "Error";

    public static final String INSTRUCTOR_FEEDBACK_SESSION_VISIBLE_TIME_CUSTOM = "custom";
    public static final String INSTRUCTOR_FEEDBACK_SESSION_VISIBLE_TIME_ATOPEN = "atopen";
    public static final String INSTRUCTOR_FEEDBACK_SESSION_VISIBLE_TIME_NEVER = "never";
    
    public static final String INSTRUCTOR_FEEDBACK_RESULTS_VISIBLE_TIME_CUSTOM = "custom";
    public static final String INSTRUCTOR_FEEDBACK_RESULTS_VISIBLE_TIME_ATVISIBLE = "atvisible";
    public static final String INSTRUCTOR_FEEDBACK_RESULTS_VISIBLE_TIME_LATER = "later";
    public static final String INSTRUCTOR_FEEDBACK_RESULTS_VISIBLE_TIME_NEVER = "never";
    
    public static final String STUDENT_COURSE_STATUS_YET_TO_JOIN = "Yet to join";
    public static final String STUDENT_COURSE_STATUS_JOINED = "Joined";
    public static final String STUDENT_PROFILE_FIELD_NOT_FILLED = "Not Specified";
    
    public static final String USER_NAME_FOR_SELF = "Myself";
    public static final String USER_TEAM_FOR_INSTRUCTOR = "Instructors";
    
    public static String ACTION_RESULT_FAILURE = "Servlet Action Failure";
    public static String ACTION_RESULT_SYSTEM_ERROR_REPORT = "System Error Report";
    
    //for course sorting in instructorHomePage
    public static final String SORT_BY_COURSE_ID = "id";
    public static final String SORT_BY_COURSE_NAME = "name";
    public static final String SORT_BY_COURSE_CREATION_DATE = "createdAt"; 
    public static final String DEFAULT_SORT_CRITERIA = SORT_BY_COURSE_CREATION_DATE;

    public static final String DEFAULT_SECTION = "None";
    
    public static final String EVAL_PREFIX_FOR_INSTRUCTOR_PRIVILEGES = "eval%";
    /* These constants are used as variable values to mean that the variable 
     * is in a 'special' state.
     */
    @SuppressWarnings("unused")
    private void _______values_with_special_meanings________________________(){}
    
    public static final int INT_UNINITIALIZED = -9999;
    public static final double DOUBLE_UNINITIALIZED = -9999.0;
    
    public static final int MAX_POSSIBLE_RECIPIENTS = -100;
    
    public static final int POINTS_NOT_SURE = -101;
    public static final int POINTS_NOT_SUBMITTED = -999;
    
    public static final int VISIBILITY_TABLE_GIVER = 0;
    public static final int VISIBILITY_TABLE_RECIPIENT = 1;
    
    public static final String GENERAL_QUESTION = "%GENERAL%";
    public static final String USER_IS_TEAM = "%TEAM%";
    public static final String USER_IS_NOBODY = "%NOBODY%";
    
    public static final Date TIME_REPRESENTS_FOLLOW_OPENING;
    public static final Date TIME_REPRESENTS_FOLLOW_VISIBLE;
    public static final Date TIME_REPRESENTS_NEVER;
    public static final Date TIME_REPRESENTS_LATER;
    public static final Date TIME_REPRESENTS_NOW;
    
    public static final double[] TIME_ZONE_VALUES = { -12, -11, -10, -9, -8,
            -7, -6, -5, -4.5, -4, -3.5, -3, -2, -1, 0, 1, 2, 3, 3.5, 4, 4.5, 5,
            5.75, 6, 7, 8, 9, 10, 11, 12, 13 };
    
    static {
        TIME_REPRESENTS_FOLLOW_OPENING = TimeHelper.convertToDate("1970-12-31 00:00 AM UTC");
        TIME_REPRESENTS_FOLLOW_VISIBLE = TimeHelper.convertToDate("1970-06-22 00:00 AM UTC");
        TIME_REPRESENTS_NEVER = TimeHelper.convertToDate("1970-11-27 00:00 AM UTC");
        TIME_REPRESENTS_LATER = TimeHelper.convertToDate("1970-01-01 00:00 AM UTC");
        TIME_REPRESENTS_NOW = TimeHelper.convertToDate("1970-02-14 00:00 AM UTC");
    }
    
    /* Other Constants
     */
    @SuppressWarnings("unused")
    private void _______other_constants________________________(){}
    
    //Used for Feedback CONTRIBUTION question
    public static final String EQUAL_SHARE = "Equal Share";
    public static final String NOT_SURE = "Not Sure";
    public static final List<String> FEEDBACK_CONTRIBUTION_OPTIONS = Arrays.asList(
            "0%",
            EQUAL_SHARE+ " - 90%",
            EQUAL_SHARE+ " - 80%",
            EQUAL_SHARE+ " - 70%",
            EQUAL_SHARE+ " - 60%",
            EQUAL_SHARE+ " - 50%",
            EQUAL_SHARE+ " - 40%",
            EQUAL_SHARE+ " - 30%",
            EQUAL_SHARE+ " - 20%",
            EQUAL_SHARE+ " - 10%",
            EQUAL_SHARE,
            EQUAL_SHARE+ " + 10%",
            EQUAL_SHARE+ " + 20%",
            EQUAL_SHARE+ " + 30%",
            EQUAL_SHARE+ " + 40%",
            EQUAL_SHARE+ " + 50%",
            EQUAL_SHARE+ " + 60%",
            EQUAL_SHARE+ " + 70%",
            EQUAL_SHARE+ " + 80%",
            EQUAL_SHARE+ " + 90%",
            EQUAL_SHARE+ " + 100%",
            NOT_SURE);
    
    
}
