package teammates.test.cases.ui;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.datatransfer.StudentAttributes;
import teammates.common.util.Const;
import teammates.ui.controller.InstructorCourseStudentDetailsPageAction;
import teammates.ui.controller.InstructorCourseStudentDetailsPageData;
import teammates.ui.controller.ShowPageResult;

public class InstructorCourseStudentDetailsPageActionTest extends BaseActionTest {

    private final DataBundle dataBundle = getTypicalDataBundle();
    
    
    @BeforeClass
    public static void classSetUp() throws Exception {
        printTestClassHeader();
		removeAndRestoreTypicalDataInDatastore();
        uri = Const.ActionURIs.INSTRUCTOR_COURSE_STUDENT_DETAILS_PAGE;
    }
    
    @Test
    public void testExecuteAndPostProcess() throws Exception{
        
        InstructorAttributes instructor1OfCourse1 = dataBundle.instructors.get("instructor1OfCourse1");
        StudentAttributes student1InCourse1 = dataBundle.students.get("student1InCourse1");
        
        String instructorId = instructor1OfCourse1.googleId;
        gaeSimulation.loginAsInstructor(instructorId);
        
        ______TS("Invalid parameters");
        
        //no parameters
        verifyAssumptionFailure();
        
        //null student email
        String[] invalidParams = new String[]{
                Const.ParamsNames.COURSE_ID, instructor1OfCourse1.courseId
        };
        verifyAssumptionFailure(invalidParams);
        
        //null course id
        invalidParams = new String[]{
                Const.ParamsNames.STUDENT_EMAIL, student1InCourse1.email
        };
        verifyAssumptionFailure(invalidParams);
        
        
        ______TS("Typical case, view student detail");

        String[] submissionParams = new String[]{
                Const.ParamsNames.COURSE_ID, instructor1OfCourse1.courseId,
                Const.ParamsNames.STUDENT_EMAIL, student1InCourse1.email
        };
        
        InstructorCourseStudentDetailsPageAction a = getAction(submissionParams);
        ShowPageResult r = getShowPageResult(a);
        
        assertEquals(Const.ViewURIs.INSTRUCTOR_COURSE_STUDENT_DETAILS+"?error=false&" +
                "user=idOfInstructor1OfCourse1", r.getDestinationWithParams());
        assertEquals(false, r.isError);
        assertEquals("", r.getStatusMessage());
        
        InstructorCourseStudentDetailsPageData pageData = (InstructorCourseStudentDetailsPageData)r.data;
        assertEquals(instructorId, pageData.account.googleId);
        assertEquals(student1InCourse1.toString(), pageData.student.toString());
        
        String expectedLogMessage = "TEAMMATESLOG|||instructorCourseStudentDetailsPage|||instructorCourseStudentDetailsPage" +
                        "|||true|||Instructor|||Instructor 1 of Course 1|||idOfInstructor1OfCourse1" +
                        "|||instr1@course1.com|||instructorCourseStudentDetails Page Load<br>Viewing details for Student " +
                        "<span class=\"bold\">student1InCourse1@gmail.com</span> in Course " +
                        "<span class=\"bold\">[idOfTypicalCourse1]</span>" +
                        "|||/page/instructorCourseStudentDetailsPage";
        assertEquals(expectedLogMessage, a.getLogMessage());
    }
    
    private InstructorCourseStudentDetailsPageAction getAction(String... params) throws Exception{
        return (InstructorCourseStudentDetailsPageAction) (gaeSimulation.getActionObject(uri, params));
    }

}
