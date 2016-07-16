import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;
import java.util.*;

public class PianoApplet extends Applet implements ActionListener,
             TextListener
{
   public static final String HELP_MESSAGE =
   "You're probobly getting lots of errors about file permissions when you " +
   "try to play files -- But there's a way to fix it!\n\n" +
   "You'll need to edit your .java.policy file, which contains all of your " +
   "file permissions.  For 2000/XP users, it should be:\n\n" +
   "/Documents And Settings/<your username>/.java.policy\n\n" +
   "If it doesn't exist, you will need to create it.  Add the following " +
   "code:\n\n" +
   "grant {\n" +
   "   permission java.io.FilePermission \"c:\\\\<midi directory>\\\\-\", " +
   "\"read\";\n" +
   "}\n\n" +
   "Notice the double blackslash; sadly, these are necessary for all paths." +
   "  So, let's say, the midi directory you wanted was d:\\midi\\mymidi.  " +
   "You would enter:\n\n" +
   "permission java.io.FilePermission \"d:\\\\midi\\\\mymidi\\\\-\", " +
   "\"read\";\n\n" +
   "After you do all that, you should be ready to play midis in the " +
   "directory you entered, hooray!\n" +
   "You can add multiple entries to this file to play from multiple " +
   "directories.";

   private Label metronomeOnLabel = new Label ("Off");

   private Label hueLabel = new Label ("Hue: 360\u00b0");
   private Label satLabel = new Label ("Saturation: 100%");

   private Button metronomeButton = new Button ("Toggle");
   private Button rebuildButton   = new Button ("Rebuild Piano");
   private Button standardButton  = new Button ("Standard");
   private Button wholetoneButton = new Button ("Wholetone");
   private Button playButton      = new Button ("Play");
   private Button stopButton      = new Button ("Stop");
   private Button helpButton      = new Button ("Help");

   private TextField metronomeText     = new TextField ("120");
   private TextField layoutText        = new TextField ("121121211212");
   private TextField transpositionText = new TextField ("21");
   private TextField keycodeText       = new TextField ("Key");
   private TextField toneText          = new TextField ("Tone");
   private TextField instrumentText    = new TextField ("0    ");
   private TextField midiText          = new TextField ("87-15b.mid");

   private Piano piano;
   private Metronome metronome = new Metronome ();
   private Keymap keymap = new Keymap ();
   private Visual visual = new Visual (500, 200);

   private Timer timer = new Timer ();

   public class UpdateTask extends TimerTask
   {
      Color lastColor = Color.white;

      public void run ()
      {
         Color color = visual.averageColor();

         if (color.getRGB() != lastColor.getRGB()) {
            lastColor = color;

            float[] hsb = new float[3];

            Color.RGBtoHSB (color.getRed(), color.getGreen(),
                            color.getBlue(), hsb);

            if (hsb[2] == 0) {
               hueLabel.setText ("Hue: N/A");
               satLabel.setText ("Saturation: N/A");
            }
            else if (hsb[1] == 0) {
               hueLabel.setText ("Hue: N/A");
               satLabel.setText ("Saturation: " + (int) (hsb[1] * 100) + "%");
            }
            else {
               hueLabel.setText ("Hue: " + (int) (hsb[0] * 360) + "\u00b0");
               satLabel.setText ("Saturation: " + (int) (hsb[1] * 100) + "%");
            }
         }
      }
   }

   public void init ()
   {
      add (new Label ("Key Layout: "));
      add (layoutText);
      add (new Label ("Lowest Note: "));
      add (transpositionText);
      add (rebuildButton);
      add (new Label ("Preset Layouts: "));
      add (standardButton);
      add (wholetoneButton);
      add (new HorizDivider());

      add (new Label ("Metronome: "));
      add (metronomeOnLabel);
      add (metronomeText);
      add (metronomeButton);
      add (new Label ("Instrument: "));
      add (instrumentText);
      add (new HorizDivider());

      add (midiText);
      add (playButton);
      add (stopButton);
      add (helpButton);
      add (new HorizDivider());

      add (visual);
      add (new HorizDivider());

      add (hueLabel);
      add (satLabel);
      add (new HorizDivider());

      try {
         piano = new Piano(layoutText.getText(), 88,
                           Integer.parseInt (transpositionText.getText()));
      }
      catch (PianoException e) {
         System.out.println (e);
         return;
      }

      add (piano);

      metronomeButton.addActionListener (this);
      rebuildButton.addActionListener (this);
      standardButton.addActionListener (this);
      wholetoneButton.addActionListener (this);
      playButton.addActionListener (this);
      stopButton.addActionListener (this);
      helpButton.addActionListener (this);

      metronomeText.addTextListener (this);
      instrumentText.addTextListener (this);

      keymap.usePreset (Keymap.STANDARD);
      piano.assignKeymap (keymap);
      piano.assignVisual (visual);

      timer.scheduleAtFixedRate (new UpdateTask(), 0, 25);
   }

   public void actionPerformed (ActionEvent event)
   {
      if (event.getSource() == metronomeButton) {
         if (metronome.isEnabled()) {
            metronomeOnLabel.setText ("Off");
            metronome.disable ();
         }
         else {
            metronomeOnLabel.setText ("On");
            metronome.enable (Integer.parseInt (metronomeText.getText()));
         }
      }
      else if (event.getSource() == rebuildButton) {
         try {
            piano.setLayout (layoutText.getText(), 88,
                             Integer.parseInt (transpositionText.getText()));
         }
         catch (PianoException e) {
            System.out.println (e);
         }
      }
      else if (event.getSource() == standardButton) {
         try {
            layoutText.setText ("121121211212");
            transpositionText.setText ("21");
            piano.setLayout (layoutText.getText(), 88,
                             Integer.parseInt (transpositionText.getText()));

            keymap.usePreset (Keymap.STANDARD);
         }
         catch (PianoException e) {
            System.out.println (e);
         }
      }
      else if (event.getSource() == wholetoneButton) {
         try {
            layoutText.setText ("12");
            transpositionText.setText ("21");
            piano.setLayout (layoutText.getText(), 88,
                             Integer.parseInt (transpositionText.getText()));

            keymap.usePreset (Keymap.WHOLETONE);
         }
         catch (PianoException e) {
            System.out.println (e);
         }
      }
      else if (event.getSource() == playButton)
         piano.play (midiText.getText());
      else if (event.getSource() == stopButton)
         piano.stop ();
      else if (event.getSource() == helpButton) {
         Container container = this.getParent();

         while (!(container instanceof Frame))
            container = container.getParent();
         Frame parent = (Frame) container;

         HelpDialog d = new HelpDialog (parent, HELP_MESSAGE);

         d.show ();
      }
   }

   public void textValueChanged (TextEvent event)
   {
      int bpm, instr;

      if (event.getSource() == metronomeText && metronome.isEnabled()) {
         try {
            bpm = Integer.parseInt (metronomeText.getText ());
         }
         catch (Exception e) {
            bpm = 1;
         }
         metronome.enable (bpm);
      }
      else if (event.getSource() == instrumentText) {
         try {
            instr = Integer.parseInt (instrumentText.getText ());
         }
         catch (Exception e) {
            instr = 1;
         }
         piano.setMidi (0, instr);
      }
   }
}
