package locales;

import java.util.ListResourceBundle;

/**
 * 
 * @author Vladimír Župka 2016
 */
public class L_ButtonBundle_cs_CZ extends ListResourceBundle{
   @Override
   public Object[][] getContents() {
      return contents;
    }

    final private Object[][] contents = {
       // U_Menu
       {"Run", "Spustit"},
       {"Param", "Parametry"},
 
       // U_ParametersEdit
       {"Sav", "Uložit data"},  
       {"SelectFile", "Vyberte soubor"},  
       {"SelectCharset", "Vyberte charset"},  

       // U_DataTable list
       {"Exit", "Končit"},   
       {"Edit_sel", "Zobrazit vybraný"},       
       {"Refresh", "Obnovit zobrazení"},         
       {"Columns", "Výběr sloupců"},       
       
       // U_DataTable data
       {"Return", "Návrat"},       

       // U_ColumnJList
       {"CopyCol", ">>>"},       
       {"DeleteCol", "X<<"},       
       {"ClearAll", "Vymazat vše"},       
       {"SaveExit", "Uložit + návrat"},       
    };
}
