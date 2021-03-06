import java.io.*;

public class Keymap implements Serializable
{
   // Internal key values.
   public static final int KEY_UNASSIGNED            = -1;
   public static final int KEY_PEDAL_ON              = -2;
   public static final int KEY_PEDAL_OFF             = -3;
   public static final int KEY_PEDAL_TOGGLE          = -4;
   public static final int KEY_VOLUME_UP             = -5;
   public static final int KEY_VOLUME_DOWN           = -6;
   public static final int KEY_VOLUME_DEFAULT        = -7;
   public static final int KEY_TRANSPOSE_UP          = -8;
   public static final int KEY_TRANSPOSE_DOWN        = -9;
   public static final int KEY_TRANSPOSE_OCTAVE_UP   = -10;
   public static final int KEY_TRANSPOSE_OCTAVE_DOWN = -11;
   public static final int KEY_TRANSPOSE_DEFAULT     = -12;
   public static final int KEY_ALL_NOTES_OFF         = -13;

   // Key layout constants.
   public static final int    KEY_LAYOUT_STANDARD        = 0;
   public static final int    KEY_LAYOUT_STANDARD_LOW    = 21;
   public static final String KEY_LAYOUT_STANDARD_ORDER  = "121121211212";
   public static final int    KEY_LAYOUT_WHOLETONE       = 1;
   public static final int    KEY_LAYOUT_WHOLETONE_LOW   = 21;
   public static final String KEY_LAYOUT_WHOLETONE_ORDER = "12";

   // Table of keys pressed and their values.
   private int[] keyTable;

   public Keymap ()
   {
      // Initialize key table.
      keyTable = new int[Piano.TONE_RANGE];
      clear ();
   }

   public void assign (String string, int tone)
   {
      assign (string.toUpperCase().toCharArray()[0], tone);
   }

   public void assign (int keycode, int tone)
   {
      // TODO: throw exceptions for invalid keycodes/tones.
      keyTable[keycode] = tone;
   }

   public int getTone (int keycode)
   {
      // TODO: throw exceptions for invalid keycodes/tones.
      return keyTable[keycode];
   }

   public boolean keyAssigned (int keycode)
   {
      // TODO: throw exceptions for invalid keycodes/tones.
      if (keyTable[keycode] != KEY_UNASSIGNED)
         return true;
      return false;
   }

   public void clear ()
   {
      for (int i = 0; i < Piano.TONE_RANGE; i++)
         keyTable[i] = KEY_UNASSIGNED;
   }

   public void usePreset (int preset)
   {
      // Clear keyboard layout.
      clear ();

      // Assign tones.
      switch (preset) {
         case KEY_LAYOUT_STANDARD:
            assign ("A", 8);  assign ("Z", 9);  assign ("S", 10);
            assign ("X", 11); assign ("C", 12); assign ("F", 13);
            assign ("V", 14); assign ("G", 15); assign ("B", 16);
            assign ("N", 17); assign ("J", 18); assign ("M", 19);
            assign ("K", 20); assign (",", 21); assign ("L", 22);
            assign (".", 23); assign ("/", 24); assign ("1", 25);
            assign ("Q", 26); assign ("2", 27); assign ("W", 28);
            assign ("E", 29); assign ("4", 30); assign ("R", 31);
            assign ("5", 32); assign ("T", 33); assign ("6", 34);
            assign ("Y", 35); assign ("U", 36); assign ("8", 37);
            assign ("I", 38); assign ("9", 39); assign ("O", 40);
            assign ("P", 41);
            break;

         case KEY_LAYOUT_WHOLETONE:
            assign ("A", 8);  assign ("Z", 9);  assign ("S", 10);
            assign ("X", 11); assign ("D", 12); assign ("C", 13);
            assign ("F", 14); assign ("V", 15); assign ("G", 16);
            assign ("B", 17); assign ("H", 18); assign ("N", 19);
            assign ("J", 20); assign ("M", 21); assign ("K", 22);
            assign (",", 23); assign ("L", 24); assign (".", 25);
            assign (";", 26); assign ("/", 27); assign ("1", 28);
            assign ("Q", 29); assign ("2", 30); assign ("W", 31);
            assign ("3", 32); assign ("E", 33); assign ("4", 34);
            assign ("R", 35); assign ("5", 36); assign ("T", 37);
            assign ("6", 38); assign ("Y", 39); assign ("7", 40);
            assign ("U", 41); assign ("8", 42); assign ("I", 43);
            assign ("9", 44); assign ("O", 45); assign ("0", 46);
            assign ("P", 47);
            break;
      }

      // Set special keys.
      assign ("-",  KEY_TRANSPOSE_OCTAVE_DOWN);
      assign ("=",  KEY_TRANSPOSE_OCTAVE_UP);
      assign ("[",  KEY_TRANSPOSE_DOWN);
      assign ("]",  KEY_TRANSPOSE_UP);
      assign ("\\", KEY_TRANSPOSE_DEFAULT);
      assign (" ",  KEY_PEDAL_TOGGLE);
      assign ("`",  KEY_PEDAL_TOGGLE);
   }
}
