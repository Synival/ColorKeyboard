import javax.swing.*;
import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class Piano extends JComponent
{
   // Default key dimensions, in pixels.
   static public final int DEFAULT_LOW_WIDTH = 12;
   static public final int DEFAULT_LOW_HEIGHT = 30;
   static public final int DEFAULT_HIGH_WIDTH = 6;
   static public final int DEFAULT_HIGH_HEIGHT = 30;
   static public final int DEFAULT_HIGH_OFFSET = 9;

   // Values for keyOff[].
   static public final int KEY_OFF_WAITING  = 0x01;
   static public final int KEY_OFF_SOUND    = 0x02;
   static public final int KEY_OFF_EAT_ONE  = 0x04;

   // Other default options.
   static public final int DEFAULT_KEYS       = 88;
   static public final int DEFAULT_TRANSPOSE  = 36;

   // Internal values and flags.
   static public final int MOUSE_RELEASED = -1;
   static public final int MASK_MOUSE     = 1 << 0;
   static public final int MASK_KEYBOARD  = 1 << 1;
   static public final int MASK_PLAYBACK  = 1 << 2;
   static public final int TONE_RANGE     = 256;

   // MIDI information.
   private int channel;
   private int instrument;
   private MidiChannel[] mc;
   private Synthesizer synth;
   private Sequencer sequencer;

   // Performance settings.
   private int transpose = DEFAULT_TRANSPOSE;
   private boolean pedal = false;

   // Keyboard layout.
   private String keyLayout;
   private int keys;
   private int lowestNote;
   private Key[] keyTable;

   // Computer keyboard mappings.
   private Keymap keymap = null;

   // Visualization data.
   private Visual visual = null;

   // Keypress data.
   private int mouseDown = MOUSE_RELEASED;
   private int[] keyMask;

   // Key draw information.
   private int highWidth, highHeight, highOffset;
   private int lowWidth,  lowHeight;

   // MIDI playback data.
   private Sequence sequence;
   private Track[] tracks;
   private int[] trackEvent;

   // MIDI pedal information (per tone).
   private boolean[] pedalList = new boolean[TONE_RANGE];
   private int[]     keyOff    = new int[TONE_RANGE];

   // Update timer.
   private java.util.Timer timer;

   // Task run on every Update tick;
   private class UpdateTask extends TimerTask
   {
      private java.util.Timer timer;
      private float interval;

      // Basic initializer that takes information passed to
      //    Timer.scheduleAtFixedRate().
      public UpdateTask (java.util.Timer timer, float interval)
      {
         this.timer    = timer;
         this.interval = interval;
      }

      public void run ()
      {
         // Is MIDI playback active?
         long curPos = sequencer.getTickPosition();
         if (sequencer.isRunning() && sequence != null && tracks != null) {
            // Check every track for events.
            for (int i = 0; i < tracks.length; i++) {
               // Look at active event, starting at where we left off the
               // last time this loop was executed.
               int j;
               for (j = trackEvent[i]; j < tracks[i].size(); j++) {
                  // If the event isn't happening yet, break.
                  MidiEvent event = tracks[i].get(j);
                  if (event.getTick() > curPos)
                     break;

                  // Is this an event we can use?
                  MidiMessage message = event.getMessage();
                  if (!(message instanceof ShortMessage))
                     continue;
                  ShortMessage sm = (ShortMessage) message;

                  // It is - what kind of event is it?
                  switch (sm.getCommand()) {
                     // Turn notes on.
                     case ShortMessage.NOTE_ON:
                        if (sm.getData2() == 0)
                           noteOff (sm.getData1(), MASK_PLAYBACK, false);
                        else
                           noteOn (sm.getData1(), MASK_PLAYBACK, false);
                        break;

                     // Turn notes off.
                     case ShortMessage.NOTE_OFF:
                        noteOff (sm.getData1(), MASK_PLAYBACK, false);
                        break;

                     // Toggle controller changes.
                     case ShortMessage.CONTROL_CHANGE:
                        switch (sm.getData1 ()) {
                           // Pedal on/off.
                           case 0x40:
                              if (sm.getData2() >= 0x80)
                                 pedalOn ();
                              else
                                 pedalOff ();
                              break;
                           // TODO: more controllers.
                        }
                        break;
                  }
               }

               // Record that we should look at the next event
               // in this track the next time we run this loop.
               trackEvent[i] = j;
            }
         }

         // Turn keys off.
         for (int key = 0; key < TONE_RANGE; key++) {
            if (keyOff[key] == 0)
               continue;
            if ((keyOff[key] & KEY_OFF_EAT_ONE) != 0) {
               keyOff[key] &= ~KEY_OFF_EAT_ONE;
               continue;
            }

            // Redraw the key as 'off'.
            try {
               keyTable[key - lowestNote].setPressed (false);
               keyTable[key - lowestNote].paintComponent (getGraphics());
            } catch (Exception e) {
               System.out.println (e);
            }

            // If the pedal is off, turn off the note.
            if (!pedal) {
               if (visual != null)
                  visual.noteOff (key);
               if ((keyOff[key] & KEY_OFF_SOUND) != 0)
                  mc[channel].noteOff (key, 600);
            }

            // Remove 'turn key off' flags.
            keyOff[key] = 0;
         }

         // Update the visualizer.
         if (visual != null) {
            visual.rebuildMainPoly ();
            visual.run (interval);
         }
      }
   }

   public Piano () throws InvalidKeyLayoutException
   {
      // Use the "Standard" layout by default.  This looks like a real piano.
      this (Keymap.KEY_LAYOUT_STANDARD_ORDER, Keymap.KEY_LAYOUT_STANDARD_LOW);
   }

   public Piano (String keyLayout, int lowestNote)
      throws InvalidKeyLayoutException
   {
      // Use 88 keys.
      this (DEFAULT_KEYS, keyLayout, lowestNote);
   }

   public Piano (int keys, String keyLayout, int lowestNote)
                throws InvalidKeyLayoutException
   {
      // Default render specifications.
      highWidth  = DEFAULT_HIGH_WIDTH;
      highHeight = DEFAULT_HIGH_HEIGHT;
      highOffset = DEFAULT_HIGH_OFFSET;
      lowWidth   = DEFAULT_LOW_WIDTH;
      lowHeight  = DEFAULT_LOW_HEIGHT;

      // Turn on our MIDI device.
      try {
         synth = MidiSystem.getSynthesizer ();
         synth.open ();
         mc = synth.getChannels ();
         sequencer = MidiSystem.getSequencer ();
         sequencer.open ();
      }
      catch (Exception e) {
         System.out.println (e);
      }

      // Use channel 0, instrument 0 (piano).
      setMidi (0, 0);

      // Activate the layout we specified.
      setLayout (keys, keyLayout, lowestNote);

      // Check for updates and modify visualizer at 62.5fps.
      timer = new java.util.Timer ();
      timer.scheduleAtFixedRate (new UpdateTask (timer, 0.016f), 0, 16);
   }

   public void play (String file) {
      // Turn playback notes off.
      allNotesOff (MASK_PLAYBACK);

      // Attempt to play the file.
      try {
         // Does the file exist?  If so, start streaming it.
         FileInputStream f = new FileInputStream (file);
         byte[] byteBuf = new byte[0x1 << 20];
         int bytesRead = f.read (byteBuf, 0, 0x1 << 20);
         ByteArrayInputStream stream = new ByteArrayInputStream
                                           (byteBuf, 0, bytesRead);

         // Start the file.
         sequencer.setSequence (stream);
         sequencer.start ();

         // Track our MIDI sequence and its tracks.
         sequence = sequencer.getSequence ();
         tracks   = sequence.getTracks ();

         // Initialize our MIDI event tracker.  MIDI is tracked in UpdateTask.
         trackEvent = new int[tracks.length];
         for (int i = 0; i < tracks.length; i++)
            trackEvent[i] = 0;
      }
      catch (Exception e) {
         System.out.println (e);
      }
   }

   public void stop ()
   {
      // Turn off our MIDI file and stop following it.
      sequencer.stop ();
      allNotesOff (MASK_PLAYBACK);
      sequence = null;
      tracks   = null;
   }

   public void buildKeyTables ()
   {
      // Start a new key table.
      keyTable = new Key[keys];
      keyMask  = new int[TONE_RANGE];

      // Set up our state for generating keys.
      int  offset  = 0;
      char nextKey = keyLayout.toCharArray()[0];
      char lastKey = '1', key;
      Color color, onColor = new Color (0, 0, 0);

      // Create our keys, one by one.
      for (int i = 0; i < keys; i++) {
         // Use whatever tier we've earlier determined comes next.
         key = nextKey;

         // Determine what tier the next key will be.  If we're at the end,
         // assume the next tier is lower so it draws nicely.
         if (i == keys - 1)
            nextKey = '1';
         else
            nextKey = keyLayout.toCharArray()[(i + 1) % keyLayout.length()];

         // Determine key color.
         switch ((i + lowestNote) % 12) {
            // The C Major scale is always a 'white' key.
            case 0:  // C
            case 2:  // D
            case 4:  // E
            case 5:  // F
            case 7:  // G
            case 9:  // A
            case 11: // B
               color = new Color (239, 239, 239);
               break;
   
            // Everything else is a 'black' key.
            default:
               color = new Color (64, 64, 64);
         }

         // Is this a low or high tier key?
         switch (key) {
            // Lower tier.
            case '1':
               // Cut the top parts of the key depending on adjacent
               // higher-tier keys.
               int topOffset = 0, topWidth = lowWidth;
               if (lastKey == '2') {
                  topOffset += (highWidth + highOffset - lowWidth);
                  topWidth = lowWidth - topOffset;
               }
               if (nextKey == '2')
                  topWidth -= (lowWidth - highOffset);

               // Make the key.
               keyTable[i] = new Key (offset * lowWidth, 0, topOffset,
                             topWidth, highHeight, lowWidth, lowHeight,
                             color, onColor);
               break;

            // Higher tier.
            case '2':
               keyTable[i] = new Key (offset * lowWidth + highOffset, 0,
                             0, highWidth, highHeight, lowWidth, 0, color,
                             onColor);
               break;
         }

         // If this is a lower-tier key, move forward.
         if (nextKey == '1')
            offset++;

         // Remember what key this was.
         lastKey = key;
      }
   }

   public void paint (Graphics g)
   {
      // Draw each key individually.
      for (int i = 0; i < keys; i++)
         keyTable[i].paintComponent (g);
   }

   public Dimension getPreferredSize() {
      return new Dimension (getWidth(), getHeight());
   }

   public int getWidth ()
   {
      int width = lowWidth;
      char[] layout = keyLayout.toCharArray();

      // Add the width of all lower-tiered keys.
      for (int i = 0; i < keys; i++)
         if (layout[(i + lowestNote) % keyLayout.length()] == '1')
            width += lowWidth;

      return width;
   }

   public int getHeight ()
   {
      return highHeight + lowHeight;
   }

   protected void processKeyEvent (KeyEvent event)
   {
      if (keymap == null)
         return;

      // Does our key correspond to a keyboard tone?
      int tone = keymap.getTone (event.getKeyCode());
      if (tone >= 0) {
         tone += transpose;
         if (tone >= Piano.TONE_RANGE || tone < 0)
            return;

         // Turn keys on/off.
         switch (event.getID()) {
            case KeyEvent.KEY_PRESSED:
               noteOn (tone, MASK_KEYBOARD);
               break;
            case KeyEvent.KEY_RELEASED:
               noteOff (tone, MASK_KEYBOARD);
               break;
         }
      }
      // Not a tone - must be a special key.
      else if (event.getID() == KeyEvent.KEY_PRESSED) {
         switch (tone) {
            // Pedal controls.
            case Keymap.KEY_PEDAL_ON:
               pedalOn ();
               break;
            case Keymap.KEY_PEDAL_OFF:
               pedalOff ();
               break;
            case Keymap.KEY_PEDAL_TOGGLE:
               if (pedal)
                  pedalOff ();
               else
                  pedalOn ();
               break;

            // Volume adjustment.
            case Keymap.KEY_VOLUME_UP:
            case Keymap.KEY_VOLUME_DOWN:
            case Keymap.KEY_VOLUME_DEFAULT:
               // TODO: write me!
               break;

            // Transposition.
            case Keymap.KEY_TRANSPOSE_UP:
               transpose++;
               break;
            case Keymap.KEY_TRANSPOSE_DOWN:
               transpose--;
               break;
            case Keymap.KEY_TRANSPOSE_OCTAVE_UP:
               transpose += 12;
               break;
            case Keymap.KEY_TRANSPOSE_OCTAVE_DOWN:
               transpose -= 12;
               break;
            case Keymap.KEY_TRANSPOSE_DEFAULT:
               transpose = DEFAULT_TRANSPOSE;
               break;

            // Emergency button.
            case Keymap.KEY_ALL_NOTES_OFF:
               allNotesOff (255);
               break;
         }
      }
   }

   public void pedalOn ()
   { 
      // Turn the pedal on for every key.
      pedal = true;
      for (int key = 0; key < TONE_RANGE; key++)
         if (keyMask[key] > 0)
            pedalList[key] = true;
   }

   public void pedalOff ()
   {
      // Turn the pedal off for every key.
      pedal = false;
      for (int key = 0; key < TONE_RANGE; key++) {
         if (pedalList[key] == true) {
            // Turn MIDI/visualizer notes off.
            if (keyMask[key] == 0) {
               mc[channel].noteOff (key, 600);
               visual.noteOff (key);
            }
            pedalList[key] = false;
         }
      }
   }

   protected void processMouseEvent (MouseEvent event)
   {
      switch (event.getID()) {
         // Turn notes on.
         case MouseEvent.MOUSE_PRESSED:
            this.requestFocusInWindow ();
            for (int i = 0; i < keys; i++) {
               if (keyTable[i].mouseInBounds (event.getX(), event.getY())) {
                  setMouseDown (i + lowestNote);
                  return;
               }
            }
            break;

         // Turn notes off.
         case MouseEvent.MOUSE_RELEASED:
         case MouseEvent.MOUSE_EXITED:
            setMouseDown (MOUSE_RELEASED);
            break;
      }
   }

   protected void processMouseMotionEvent (MouseEvent event)
   {
      // We moved the mouse; what key are we under now?
      int x = event.getX(), y = event.getY();
      if (mouseDown != MOUSE_RELEASED) {
         for (int i = 0; i < keys; i++) {
            if (keyTable[i].mouseInBounds (x, y)) {
               setMouseDown (i + lowestNote);
               return;
            }
         }
      }

      // We're not under anything - turn the key off.
      setMouseDown (MOUSE_RELEASED);
   }

   private void setMouseDown (int key)
   {
      // Don't do anything if the key we want is already down.
      if (key == mouseDown)
         return;

      // Are we turning a key off?
      if (mouseDown != MOUSE_RELEASED)
         noteOff (mouseDown, MASK_MOUSE);

      // Are we turning a key on?
      if (key != MOUSE_RELEASED)
         noteOn (key, MASK_MOUSE);

      // Record the key.
      mouseDown = key;
   }

   void noteOn (int key, int mask)
   {
      // Play a sound.
      noteOn (key, mask, true);
   }

   void noteOff (int key, int mask)
   {
      // Turn a sound off.
      noteOff (key, mask, true);
   }

   void noteOn (int key, int mask, boolean sound)
   {
      // Don't bother if it's already on.
      if ((keyMask[key] & mask) == mask)
         return;

      // If the key is waiting to be turned off, it's already on.  Ignore
      // the next block of code and remove the 'turn off' flag.
      if (keyOff[key] != 0)
         keyOff[key] = 0;
      // If this is the first event to turn a key on, play a sound and
      // turn on the visualizer.
      else if (keyMask[key] == 0) {
         // Redraw the key as 'on'.
         try {
            keyTable[key - lowestNote].setPressed (true);
            keyTable[key - lowestNote].paintComponent (getGraphics());
         } catch (Exception e) { }

         // Play a sound.
         if (sound)
            mc[channel].noteOn (key, 600);

         // Turn on visualizer.
         if (visual != null)
            visual.noteOn (key);

         // Is the pedal on?  This key should persist.
         if (pedal)
            pedalList[key] = true;
      }

      // Mark the key on as key type 'mask'.
      keyMask[key] |= mask;
   }

   void noteOff (int key, int mask, boolean sound)
   {
      // Don't bother if it's already off.
      if ((keyMask[key] & mask) == 0)
         return;

      // Unmark the key as key type 'mask'.
      keyMask[key] &= ~mask;

      // If we're turning the key off, let the timer do it.
      if (keyMask[key] == 0) {
         keyOff[key] = KEY_OFF_WAITING | KEY_OFF_EAT_ONE;
         if (sound)
            keyOff[key] |= KEY_OFF_SOUND;
      }
   }

   void allNotesOff (int mask)
   {
      // Be safe; no pedal.
      pedalOff ();

      // Turn notes off.
      for (int key = 0; key < TONE_RANGE; key++)
         if ((keyMask[key] & mask) > 0)
            noteOff (key, mask);
   }

   public void setLayout (int keys, String keyLayout, int lowestNote)
                          throws InvalidKeyLayoutException
   {
      // TODO: throw exceptions for # of keys and lowest note
      if (!validKeyLayout (keyLayout))
         throw new InvalidKeyLayoutException ();

      // Have we updated?
      boolean update = false;
      if (this.keys != keys || this.lowestNote != lowestNote)
         update = true;

      // Update internal variables.
      this.keyLayout = keyLayout;
      this.keys = keys;
      this.lowestNote = lowestNote;

      // Rebuild keys and make sure key events are on.
      buildKeyTables ();
      enableEvents (AWTEvent.MOUSE_MOTION_EVENT_MASK |
                    AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK |
                    AWTEvent.FOCUS_EVENT_MASK);

      // Resize, which will automatically repaint.  If there was no size
      // change, make sure we update anyway.
      Dimension oldSize = getSize ();
      setSize (getPreferredSize ());

      // If we've changed size, update the layout.
      if (getSize().getWidth() != oldSize.getWidth()) {
         if (getParent() != null)
            getParent().doLayout ();
      }
      // Otherwise, if we updated, we haven't redrawn - make sure we do that.
      else if (update)
         repaint ();
   }

   public void assignKeymap (Keymap keymap)
   {
      this.keymap = keymap;
   }

   public void assignVisual (Visual visual)
   {
      this.visual = visual;
   }

   public void setMidi (int channel, int instrument)
   {
      // Remember our channel and instrument.
      this.channel    = channel;
      this.instrument = instrument;

      // Use the instrument requested.
      try {
         mc[channel].programChange (instrument);
      }
      catch (Exception e) {
         System.out.println (e);
      }
   }

   static public boolean validKeyLayout (String keyLayout)
   {
      // Empty strings are invalid.
      if (keyLayout.length() == 0)
         return false;

      // Test for consequtive 2nd tier keys.
      if (keyLayout.indexOf("22") != -1)
         return false;
      else if (keyLayout.startsWith("2") && keyLayout.endsWith("2"))
         return false;

      // Test for invalid characters.
      for (int i = 0; i < keyLayout.length(); i++) {
         switch (keyLayout.toCharArray()[i]) {
            case '1':
            case '2':
               break;
            default:
               return false;
         }
      }

      // Looks like it works.
      return true;
   }

   public boolean isFocusable ()
   {
      return true;
   }

   public void ProcessFocusEvent (FocusEvent e)
   {
      // Turn off keyboard and mouse events if we lose focus.
      if (e.getID() == FocusEvent.FOCUS_LOST)
         allNotesOff (MASK_MOUSE | MASK_KEYBOARD);
   }
}
