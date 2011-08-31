import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.LifecycleUtils;
import org.apache.shiro.util.ThreadState;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.junit.AfterClass;

public class ShiroEnvironment {

  private static ThreadState subjectThreadState;

    public void setSubject(Subject subject) {
        clearSubject();
        subjectThreadState = createThreadState(subject);
        subjectThreadState.bind();
    }

    public Subject getSubject() {
        return SecurityUtils.getSubject();
    }

    public ThreadState createThreadState(Subject subject) {
        return new SubjectThreadState(subject);
    }

    public void clearSubject() {
        doClearSubject();
    }

    private static void doClearSubject() {
        if (subjectThreadState != null) {
            subjectThreadState.clear();
            subjectThreadState = null;
        }
    }

    public static void setSecurityManager(SecurityManager securityManager) {
        SecurityUtils.setSecurityManager(securityManager);
    }

    public static SecurityManager getSecurityManager() {
        return SecurityUtils.getSecurityManager();
    }

    public void tearDownShiro() {
        doClearSubject();
        try {
            SecurityManager securityManager = getSecurityManager();
            LifecycleUtils.destroy(securityManager);
        } catch (UnavailableSecurityManagerException e) {
            //we don't care about this when cleaning up the test environment
            //(for example, maybe the subclass is a unit test and it didn't
            // need a SecurityManager instance because it was using only 
            // mock Subject instances)
        }
        setSecurityManager(null);
    }
}