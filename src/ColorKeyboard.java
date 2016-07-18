import javax.swing.JFrame;

public class ColorKeyboard {
   public static void main (String[] args)
   {
      PianoFrame piano = new PianoFrame ();
      piano.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
      piano.setVisible (true);
   }
}
