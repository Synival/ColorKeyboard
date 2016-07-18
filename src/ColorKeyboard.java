import javax.swing.JFrame;

// Entry point for project.
public class ColorKeyboard {
   public static void main (String[] args)
   {
      // Create a piano frame and center it.  The piano keyboard will be
      // forcused automatically.
      PianoFrame piano = new PianoFrame ();
      piano.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
      piano.setLocationRelativeTo (null);
      piano.setVisible (true);
   }
}
