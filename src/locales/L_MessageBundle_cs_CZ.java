package locales;

import java.util.ListResourceBundle;

/**
 * 
 * @author Vladimír Župka 2016
 */
public class L_MessageBundle_cs_CZ extends ListResourceBundle {
   @Override
   public Object[][] getContents() {
      return contents;
   }

   final private Object[][] contents = {
         // U_MainWindow
         { "CurDir", "Běžný adresář je: " },
         { "NoConnection", "NENÍ SPOJENÍ S DATABÁZOVÝM SERVEREM!" },
         { "InvalSchema", "CHYBNÉ SCHEMA/TABULKA " },
         { "CorrectPar", "Opravte aplikační parametry nebo zkontrolujte soubor a jeho členy." },

         // U_ParametersEdit
         { "ParSaved", "Parametry byly uloženy do souboru: " },
         { "InvalFile", "Chybné jméno souboru!" },
         { "InvalCharset", "Chybný kód znakové sady!" },
         { "ConnLost", "CHYBA V SQL PŘÍKAZU nebo SPOJENÍ SE SERVEREM ZTRACENO." },

         // U_ConnectDB
         { "Driver", "Nebyl nalezen ovladač JDBC k serveru " },
         { "ConnErr", "Chyba spojení: " },
         
         // U_DataTable
         { "NoRowUpd", "Není vybrán žádný řádek. Vyberte jeden k úpravě." },

         // U_DataTable, getData()
         { "SqlError", "Chyba v SQL příkazu: " },
         { "InvalidValue", "Nepřijatelná hodnota pro sloupec " },
         { "InvalidCharset", "Chybná znaková sada" },
         { "DataError", "Chyba vstupních dat: " },
         { "NoData", "Chybí data. Opravte hodnoty pro WHERE/ORDER BY "
               + "nebo opravte jméno tabulky v Parametrech." },
   };
}
