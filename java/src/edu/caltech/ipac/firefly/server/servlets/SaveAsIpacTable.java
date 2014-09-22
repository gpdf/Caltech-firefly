package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Date: Sep 15, 2008
 *
 * @author loi
 * @version $Id: SaveAsIpacTable.java,v 1.1 2010/11/03 20:49:36 loi Exp $
 */
public class SaveAsIpacTable  extends BaseHttpServlet {
    public static final Logger.LoggerImpl DL_LOGGER = Logger.getLogger(Logger.DOWNLOAD_LOGGER);

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String reqStr = req.getParameter(Request.class.getName());
        TableServerRequest request = reqStr == null ? null : TableServerRequest.parse(reqStr);

        if (request == null) throw new IllegalArgumentException("Invalid request");

        String fileName = req.getParameter("file_name");

        fileName = StringUtils.isEmpty(fileName) ? request.getRequestId() : fileName;
        res.setHeader("Content-Disposition", "attachment; filename=" + fileName + (fileName.endsWith(".tbl")?"":".tbl"));
        SearchManager am = new SearchManager();
        try {
            am.save(res.getOutputStream(), request);
        } finally {
            FileInfo fi = am.getFileInfo(request);
            long length = 0;
            if (fi != null) {
                length = fi.getSizeInBytes();
            }
            logStats(length, "fileName", fi.getExternalName());
            // maintain counters for applicaiton monitoring
            Counters.getInstance().increment(Counters.Category.Download, "SaveAsIpacTable");
            Counters.getInstance().incrementKB(Counters.Category.Download, "SaveAsIpacTable (KB)", length/1024);
        }
    }

    protected void logStats(long fileSize, Object... params) {
        DL_LOGGER.stats("SaveAsIpacTable", "fsize(MB)", (double) fileSize / StringUtils.MEG,
                "params", CollectionUtil.toString(params, ","));
    }


}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
