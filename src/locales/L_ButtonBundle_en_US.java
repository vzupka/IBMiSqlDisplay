package locales;

import java.util.ListResourceBundle;
/**
 * 
 * @author Vladimír Župka 2016
 */
public class L_ButtonBundle_en_US extends ListResourceBundle{
   @Override
   public Object[][] getContents() {
      return contents;
    }

    final private Object[][] contents = {
       // U_Menu
       {"Run", "Run"},
       {"Param", "Parameters"},

       // U_ParametersEdit
       {"Sav", "Save data"}, 
       {"SelectFile", "Select file"},  
       {"SelectCharset", "Select charset"},  

       // U_DataTable list
       {"Exit", "Exit"},    
       {"Edit_sel", "Display selected"},       
       {"Refresh", "Refresh"},  
       {"Columns", "Select columns"},       
       
       // U_DataTable data
       {"Return", "Return"},       

       // U_ColumnJList
       {"CopyCol", ">>>"},       
       {"DeleteCol", "X<<"},       
       {"ClearAll", "Clear all"},       
       {"SaveExit", "Save + return"},       
    };
}
