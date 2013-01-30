package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.firefly.server.util.DsvToDataGroup;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.IpacTableUtil;
import org.apache.commons.csv.CSVFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/**
 * Date: May 14, 2009
 *
 * @author loi
 * @version $Id: DataGroupReader.java,v 1.13 2012/11/05 18:59:59 loi Exp $
 */
public class DataGroupReader {
    public static final int MIN_PREFETCH_SIZE = AppProperties.getIntProperty("IpacTable.min.prefetch.size", 500);
    public static final String LINE_SEP = System.getProperty("line.separator");
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    public static enum Format { TSV(CSVFormat.TDF), CSV(CSVFormat.DEFAULT), IPACTABLE(null), UNKNOWN(null);
        CSVFormat type;
        Format(CSVFormat type) {this.type = type;}
    }

    public static DataGroup readAnyFormat(File inf) throws IOException {
        Format format = guessFormat(inf);
        if (format == Format.IPACTABLE) {
            return read(inf, false, false);
        } else if (format == Format.CSV || format == Format.TSV) {
            return DsvToDataGroup.parse(inf, format.type);
        } else {
            throw new IOException("Unsupported format, file:" + inf);
        }
    }
    
    public static DataGroup read(File inf) throws IOException {
        return read(inf, false);
        
    }

    public static DataGroup read(File inf, boolean readAsString) throws IOException {
        return read(inf, true, readAsString);
    }

    public static DataGroup read(File inf, boolean isFixedLength, boolean readAsString) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        List<DataGroup.Attribute> attributes = IpacTableUtil.readAttributes(reader);
        List<DataType> cols = IpacTableUtil.readColumns(reader);
        
        if (readAsString) {
            for (DataType dt : cols) {
                dt.setDataType(String.class);
            }
        }

        DataGroup dg = new DataGroup(null, cols);
        dg.beginBulkUpdate();

        for(DataGroup.Attribute a : attributes) {
            dg.addAttributes(a);
        }

        String line = null;
        int lineNum = 0;
        try {
            line = reader.readLine();
            lineNum++;
            while (line != null) {
                DataObject row = IpacTableUtil.parseRow(dg, line, isFixedLength);
                if (row != null) {
                    dg.add(row);
                }
                line = reader.readLine();
                lineNum++;
            }
        } catch(Exception e) {
            String msg = e.getMessage()+"<br>on line "+lineNum+": " + line;
            if (msg.length()>128) msg = msg.substring(0,128)+"...";
            logger.error(e, "on line "+lineNum+": " + line);
            throw new IOException(msg);
        } finally {
            reader.close();
        }

        dg.endBulkUpdate();
        dg.shrinkToFitData();
        return dg;
    }


    public static Format guessFormat(File inf) throws IOException {

        String fileExt = FileUtil.getExtension(inf);
        if (fileExt != null) {
            if (fileExt.equalsIgnoreCase("csv")) {
                return Format.CSV;
            } else if (fileExt.equalsIgnoreCase("tsv")) {
                return Format.TSV;
            } else if (fileExt.equalsIgnoreCase("tbl")) {
                return Format.IPACTABLE;
            }
        }

        int readAhead = 10;
        
        int row = 0;
        BufferedReader reader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        String line = reader.readLine();
        int[][] counts = new int[readAhead][2];
        int csvIdx = 0, tsvIdx = 1;
        while (line != null && row < readAhead) {
            if (line.startsWith("|") || line.startsWith("\\")) {
                return Format.IPACTABLE;
            }
            
            counts[row][csvIdx] = CSVFormat.DEFAULT.parse(new StringReader(line)).iterator().next().size();
            counts[row][tsvIdx] = CSVFormat.TDF.parse(new StringReader(line)).iterator().next().size();
            row++;
            line = reader.readLine();
        }

        // check csv
        int c = counts[0][csvIdx];
        boolean cMatch = true;
        for(int i = 1; i < row; i++) {
            cMatch = cMatch && counts[i][csvIdx] == c;
        }
        // check tsv
        int t = counts[0][tsvIdx];
        boolean tMatch = true;
        for(int i = 1; i < row; i++) {
            tMatch = tMatch && counts[i][tsvIdx] == t;
        }

        if (cMatch && tMatch) {
            if (t > c) {
                return Format.TSV;
            } else {
                return Format.CSV;
            }
        } else {
            if (cMatch) {
                return Format.CSV;
            } else if (tMatch) {
                return Format.TSV;
            } else {
                return Format.UNKNOWN;
            }
        }
    }

    

//====================================================================
//
//====================================================================

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
