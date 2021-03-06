/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 5/28/14
 * Time: 9:38 AM
 */


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Trey Roby
 */
public class EventMatchCriteria {

    private volatile List<EventTarget> matchList;

    public EventMatchCriteria(EventTarget... targets) {
        this.matchList= Arrays.asList(targets);
    }

    public synchronized void addMatchTarget(EventTarget target) {
        matchList= new ArrayList<EventTarget>(matchList);
        matchList.add(target);
    }


    public synchronized boolean matches(EventTarget target) {
        boolean matches=false;
        for(EventTarget  testTgt : matchList) {
            if (testTgt.matches(target)) {
                matches= true;
                break;
            }
        }
        return matches;
    }

    public synchronized EventTarget getFirstTarget() { return matchList.get(0); }

    @Override
    public boolean equals(Object obj) {
        boolean retval= false;
        if (obj instanceof EventMatchCriteria) {
            EventMatchCriteria other= (EventMatchCriteria)obj;
            retval= other.matchList.equals(matchList);
        }
        return retval;
    }

    public  static EventMatchCriteria makeSessionCriteria(String sessionID, String windowID) {
        return new EventMatchCriteria(new EventTarget.Session(sessionID, windowID));
    }

    public  static EventMatchCriteria makeWorkspaceCriteria(String workspaceID, String windowID) {
        return new EventMatchCriteria(new EventTarget.Session(workspaceID, windowID));
    }

    @Override
    public String toString() {
        return "EventMatchCriteria: "+Arrays.deepToString(matchList.toArray());
    }
}

