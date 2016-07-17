import javax.swing.*;
import javax.swing.event.*;
import java.applet.Applet;
import java.awt.event.ActionListener;
import java.awt.event.TextListener;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;

public class PianoApplet extends Applet implements ActionListener
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
   private JTextField layoutText        = new JTextField ("121121211212", 10);
   private JTextField transpositionText = new JTextField ("21", 2);
   private JButton rebuildButton        = new JButton ("Rebuild Piano");
   private JButton standardButton       = new JButton ("Standard");
   private JButton wholetoneButton      = new JButton ("Wholetone");

   // Metronome.
   private JButton metronomeButton  = new JButton ("Toggle");
   private JLabel metronomeOnLabel  = new JLabel ("Off");
   private JTextField metronomeText = new JTextField ("120", 3);
   private Metronome metronome      = new Metronome ();

   // Instrument patch.
   private JTextField instrumentText = new JTextField ("0", 3);

   // MIDI control.
   private JTextField midiText = new JTextField ("87-15b.mid", 12);
   private JButton playButton  = new JButton ("Play");
   private JButton stopButton  = new JButton ("Stop");
   private JButton helpButton  = new JButton ("Help");

   // Visualizer.
   private Visual visual   = new Visual (500, 200);
   private JLabel hueLabel = new JLabel ("Hue: 360\u00b0");
   private JLabel satLabel = new JLabel ("Saturation: 100%");

   // Piano.
   private Keymap keymap         = new Keymap ();
   private java.util.Timer timer = new java.util.Timer ();
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

   public class TextChanged implements DocumentListener
   {
      private Object source = null;
      public TextChanged (Object src)
         { source = src; }

      public void removeUpdate (DocumentEvent event)
         { changedUpdate (event); }
      public void insertUpdate (DocumentEvent event)
         { changedUpdate (event); }

      public void changedUpdate (DocumentEvent event)
      {
         // If we changed the metronome, update the value.
         // Use 120 if the value entered is bogus.
         if (source == metronomeText && metronome.isEnabled()) {
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
         else if (source == instrumentText) {
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

   public void init ()
   {
      // Preset keyboard layouts.
      add (new JLabel ("Preset Layouts: "));
      add (standardButton);
      add (wholetoneButton);
      add (new HorizDivider());

      // Custom keyboard layout.
      add (new JLabel ("Key Layout: "));
      add (layoutText);
      add (new JLabel ("Lowest Note: "));
      add (transpositionText);
      add (rebuildButton);
      add (new HorizDivider());

      // Add a metronome and instrument (MIDI patch) setting.
      add (new JLabel ("Metronome: "));
      add (metronomeOnLabel);
      add (metronomeText);
      add (metronomeButton);
      add (new JLabel ("Instrument: "));
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
      metronomeText.getDocument().addDocumentListener (
         new TextChanged (metronomeText));
      instrumentText.getDocument().addDocumentListener (
         new TextChanged (instrumentText));

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
         while (!(container instanceof JFrame))
            container = container.getParent();
         JFrame parent = (JFrame) container;
         HelpDialog d = new HelpDialog (parent, HELP_MESSAGE);
         d.setVisible (true);
      }
   }
}
