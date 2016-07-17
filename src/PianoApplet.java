import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;
import java.util.*;

public class PianoApplet extends Applet implements ActionListener,
             TextListener
{
   // Message displayed in 'help' menu.
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

   // Piano layout buttons.
   private TextField layoutText        = new TextField ("121121211212");
   private TextField transpositionText = new TextField ("21");
   private Button rebuildButton        = new Button ("Rebuild Piano");
   private Button standardButton       = new Button ("Standard");
   private Button wholetoneButton      = new Button ("Wholetone");

   // Metronome.
   private Button metronomeButton  = new Button ("Toggle");
   private Label metronomeOnLabel  = new Label ("Off");
   private TextField metronomeText = new TextField ("120");
   private Metronome metronome     = new Metronome ();

   // Instrument patch.
   private TextField instrumentText = new TextField (3);

   // MIDI control.
   private TextField midiText = new TextField ("87-15b.mid");
   private Button playButton  = new Button ("Play");
   private Button stopButton  = new Button ("Stop");
   private Button helpButton  = new Button ("Help");

   // Visualizer.
   private Visual visual  = new Visual (500, 200);
   private Label hueLabel = new Label ("Hue: 360\u00b0");
   private Label satLabel = new Label ("Saturation: 100%");

   // Piano.
   private Keymap keymap = new Keymap ();
   private Timer timer   = new Timer ();
   private Piano piano; // (Initialized in constructor)

   // Visualizer statistics (hue/saturation) update routine.
   public class UpdateTask extends TimerTask
   {
      Color lastColor = Color.white;
      public void run ()
      {
         // Has the color changed?  If not, we don't need to do anything.
         Color color = visual.averageColor();
         if (color.getRGB() == lastColor.getRGB())
            return;

         // Color changed.  Remember its value so we don't have to compute
         // this again every frame.
         lastColor = color;

         // Determine HSB.
         float[] hsb = new float[3];
         Color.RGBtoHSB (color.getRed(), color.getGreen(),
                         color.getBlue(), hsb);

         // If brightness is zero, there is no hue/saturation.
         if (hsb[2] == 0) {
            hueLabel.setText ("Hue: N/A");
            satLabel.setText ("Saturation: N/A");
         }
         // If saturation is zero, there is no hue.
         else if (hsb[1] == 0) {
            hueLabel.setText ("Hue: N/A");
            satLabel.setText ("Saturation: 0%");
         }
         // Otherwise, do some calculations.
         else {
            hueLabel.setText ("Hue: " +
               (int) Math.round (hsb[0] * 360.00) + "\u00b0");
            satLabel.setText ("Saturation: " +
               (int) Math.round (hsb[1] * 100.00) + "%");
         }
      }
   }

   public void init ()
   {
      // Create the top row of controls.
      add (new Label ("Key Layout: "));
      add (layoutText);
      add (new Label ("Lowest Note: "));
      add (transpositionText);
      add (rebuildButton);
      add (new Label ("Preset Layouts: "));
      add (standardButton);
      add (wholetoneButton);
      add (new HorizDivider());

      // Add a metronome and instrument (MIDI patch) setting.
      add (new Label ("Metronome: "));
      add (metronomeOnLabel);
      add (metronomeText);
      add (metronomeButton);
      add (new Label ("Instrument: "));
      instrumentText.setText ("0");
      add (instrumentText);
      add (new HorizDivider());

      // MIDI playback controls.
      add (midiText);
      add (playButton);
      add (stopButton);
      add (helpButton);
      add (new HorizDivider());

      // Visualizer.
      add (visual);
      add (new HorizDivider());

      // Hue & Saturation reports.
      add (hueLabel);
      add (satLabel);
      add (new HorizDivider());

      // Route all events to this class for convenience.
      metronomeButton.addActionListener (this);
      rebuildButton.addActionListener (this);
      standardButton.addActionListener (this);
      wholetoneButton.addActionListener (this);
      playButton.addActionListener (this);
      stopButton.addActionListener (this);
      helpButton.addActionListener (this);
      metronomeText.addTextListener (this);
      instrumentText.addTextListener (this);

      // Create the piano and print any errors directly to the console.
      try {
         // Use the default layout text (121211212121),
         // 88 keys, and whatever transposition is requested (default = 23).
         piano = new Piano(layoutText.getText(), 88,
                           Integer.parseInt (transpositionText.getText()));
         add (piano);

         // Assign our keymap and visualizer to the piano.
         keymap.usePreset (Keymap.STANDARD);
         piano.assignKeymap (keymap);
         piano.assignVisual (visual);

         // Listen to keyboard every 25ms.
         timer.scheduleAtFixedRate (new UpdateTask(), 0, 25);

         // Focus piano.
         piano.requestFocusInWindow ();
      }
      catch (PianoException e) {
         System.out.println (e);
         return;
      }
   }

   public void actionPerformed (ActionEvent event)
   {
      // What triggered this event?
      Object source = event.getSource ();

      // Toggle the metronome on and off.
      if (source == metronomeButton) {
         if (metronome.isEnabled()) {
            metronomeOnLabel.setText ("Off");
            metronome.disable ();
         }
         else {
            metronomeOnLabel.setText ("On");
            metronome.enable (Integer.parseInt (metronomeText.getText()));
         }
      }
      // "Rebuild Piano" button changes the key layout.
      else if (source == rebuildButton) {
         try {
            piano.setLayout (layoutText.getText(), 88,
               Integer.parseInt (transpositionText.getText()));
         }
         catch (PianoException e) {
            System.out.println (e);
         }
      }
      // "Standard" resets key layout to defaults.
      else if (source == standardButton) {
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
      // "Wholetone" uses a simple alternating "low, high" layout.
      else if (source == wholetoneButton) {
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
      // Play MIDI.
      else if (source == playButton) {
         piano.play (midiText.getText());
      }
      // Stop MIDI.
      else if (source == stopButton) {
         piano.stop ();
      }
      // Help window.
      else if (source == helpButton) {
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
      Object source = event.getSource ();

      // If we changed the metronome, update the value.
      // Use 120 if the value entered is bogus.
      if (event.getSource() == metronomeText && metronome.isEnabled()) {
         int bpm;
         try {
            bpm = Integer.parseInt (metronomeText.getText ());
         }
         catch (Exception e) {
            bpm = 120;
         }
         metronome.enable (bpm);
      }
      // If the changed the instrument, update our piano's patch.
      // Use 0 (accoustic grand piano) if the value entered is value.
      else if (event.getSource() == instrumentText) {
         int instr;
         try {
            instr = Integer.parseInt (instrumentText.getText ());
         }
         catch (Exception e) {
            instr = 0;
         }
         piano.setMidi (0, instr);
      }
   }
}
