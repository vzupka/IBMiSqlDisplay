package locales;

import java.util.ListResourceBundle;

/**
 * 
 * @author Vladimír Župka 2016
 */
public class L_MessageBundle_en_US extends ListResourceBundle {
   @Override
   public Object[][] getContents() {
      return contents;
   }

   final private Object[][] contents = {
         // U_Main
         { "CurDir", "Current directory is: " },
         { "NoConnection", "NO CONNECTION TO THE DATABASE SERVER!" },
         { "InvalSchema", "INVALID SCHEMA/TABLE " },
         { "CorrectPar", "Correct application parameters or the check the file and its members." },
         
         // U_ParametersEdit
         { "ParSaved", "Parameters have been saved to the file: " },
         { "InvalFile", "Invalid file name!" },
         { "InvalCharset", "Invalid charset code!" },
         { "ConnLost", "SQL STATEMENT ERROR or CONNECTION TO THE SERVER LOST." },

         // U_ConnectDB
         { "Driver", "JDBC driver not found for the host " },
         { "ConnErr", "Connection error: " },
         
         // U_DataTable
         { "NoRowUpd", "No row was selected. Select one to edit." },

         // U_DataTable, getData()
         { "SqlError", "SQL error: " },
         { "InvalidValue", "Unacceptable value for column " },
         { "InvalidCharset", "Invalid character set" },
         { "DataError", "Input data error: " },
         { "NoData", "No data. Correct values for WHERE/ORDER BY "
               + "or correct the table name in Parameters." },
   };
}
