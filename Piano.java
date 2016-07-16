import java.awt.*;
import java.awt.event.*;
import javax.sound.midi.*;
import java.io.*;
import java.util.*;

public class Piano extends Canvas
{
   static public final int DEFAULT_LOW_WIDTH = 12;
   static public final int DEFAULT_LOW_HEIGHT = 30;
   static public final int DEFAULT_HIGH_WIDTH = 6;
   static public final int DEFAULT_HIGH_HEIGHT = 30;
   static public final int DEFAULT_HIGH_OFFSET = 9;

   static public final int MOUSE_RELEASED = -1;
   static public final int TRANSPOSE_DEFAULT = 36;

   static public final int MASK_MOUSE    = 1 << 0;
   static public final int MASK_KEYBOARD = 1 << 1;
   static public final int MASK_PLAYBACK = 1 << 2;

   private int channel;
   private int instrument;
   private MidiChannel[] mc;
   private Synthesizer synth;
   private Sequencer sequencer;

   private int transpose = TRANSPOSE_DEFAULT;
   private boolean pedal = false;

   private String keyLayout;
   private int keys;
   private int lowestNote;
   private Key[] keyTable;

   private Keymap keymap;
   private boolean keymapAssigned = false;

   private Visual visual;
   private boolean visualAssigned = false;

   private int mouseDown = MOUSE_RELEASED;
   private int[] keyMask;

   private int highWidth, highHeight, highOffset;
   private int lowWidth, lowHeight;

   private Timer timer = new Timer ();

   private Sequence sequence;
   private Track[] tracks;
   private int[] trackEvent;

   private boolean[] pedalList = new boolean[256];

   private class UpdateTask extends TimerTask
   {
      public void run ()
      {
         long curPos = sequencer.getTickPosition();
         boolean doPaint = false;

         if (sequencer.isRunning() && sequence != null) {
            for (int i = 0; i < tracks.length; i++) {
               for (int j = trackEvent[i]; j < tracks[i].size(); j++) {
                  MidiEvent event = tracks[i].get(j);
                  MidiMessage message = event.getMessage();

                  if (event.getTick() > curPos)
                     break;

                  if (message instanceof ShortMessage) {
                     ShortMessage sm = (ShortMessage) message;

                     switch (sm.getCommand()) {
                        case ShortMessage.NOTE_ON:
                           if (sm.getData2() == 0)
                              noteOff (sm.getData1(), MASK_PLAYBACK, false);
                           else
                              noteOn (sm.getData1(), MASK_PLAYBACK, false);

                           break;
                        case ShortMessage.NOTE_OFF:
                           noteOff (sm.getData1(), MASK_PLAYBACK, false);
                           break;
                        case ShortMessage.CONTROL_CHANGE:
                           if (sm.getData1() == 0x40 && sm.getData2() == 0x7f)
                              pedalOn ();
                           else
                              pedalOff ();
                           break;
                     }
                  }

                  doPaint = true;

                  trackEvent[i] = j + 1;
               }
            }

            //if (doPaint)
               //repaint ();
         }

         if (visualAssigned) {
            visual.rebuildMainPoly ();
            visual.run ();
         }
      }
   }

   public Piano () throws InvalidKeyLayoutException
   {
      this ("121121211212");
   }

   public Piano (String keyLayout) throws InvalidKeyLayoutException
   {
      this (keyLayout, 88, 21);
   }

   public Piano (String keyLayout, int keys, int lowestNote)
                throws InvalidKeyLayoutException
   {
      highWidth  = DEFAULT_HIGH_WIDTH;
      highHeight = DEFAULT_HIGH_HEIGHT;
      highOffset = DEFAULT_HIGH_OFFSET;
      lowWidth   = DEFAULT_LOW_WIDTH;
      lowHeight  = DEFAULT_LOW_HEIGHT;

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

      setMidi (0, 0);
      setLayout (keyLayout, keys, lowestNote);

      timer.scheduleAtFixedRate (new UpdateTask(), 0, 25);
   }

   public void play (String file) {
      allNotesOff (MASK_PLAYBACK);

      try {
         FileInputStream f = new FileInputStream (file);
         byte[] byteBuf = new byte[0x1 << 20];
         int bytesRead = f.read (byteBuf, 0, 0x1 << 20);

         ByteArrayInputStream stream = new ByteArrayInputStream
                                           (byteBuf, 0, bytesRead);

         sequencer.setSequence (stream);
         sequencer.start ();

         sequence = sequencer.getSequence ();
         tracks = sequence.getTracks ();

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
      sequencer.stop ();
      allNotesOff (MASK_PLAYBACK);
   }

   public void buildKeyTables ()
   {
      int offset = 0;
      char nextKey = keyLayout.toCharArray()[0];
      char lastKey = '1', key;
      Color color, onColor = new Color (0, 0, 0);

      keyTable = new Key[keys];
      keyMask  = new int[256];

      for (int i = 0; i < keys; i++) {
         key = nextKey;

         if (i == keys - 1)
            nextKey = '1';
         else
            nextKey = keyLayout.toCharArray()[(i + 1) % keyLayout.length()];

         switch ((i + lowestNote) % 12) {
            case 0:
            case 2:
            case 4:
            case 5:
            case 7:
            case 9:
            case 11:
               color = new Color (239, 239, 239);
               break;
   
            default:
               color = new Color (64, 64, 64);
         }

         switch (key) {
            case '1':
               int topOffset = 0, topWidth = lowWidth;

               if (lastKey == '2') {
                  topOffset += (highWidth + highOffset - lowWidth);
                  topWidth = lowWidth - topOffset;
               }
               if (nextKey == '2')
                  topWidth -= (lowWidth - highOffset);

               keyTable[i] = new Key (offset * lowWidth, 0, topOffset,
                             topWidth, highHeight, lowWidth, lowHeight,
                             color, onColor);
               break;

            case '2':
               keyTable[i] = new Key (offset * lowWidth + highOffset, 0,
                             0, highWidth, highHeight, lowWidth, 0, color,
                             onColor);
               break;
         }

         lastKey = key;
         if (nextKey == '1')
            offset++;
      }
   }

   public void paint (Graphics g)
   {
      for (int i = 0; i < keys; i++)
         keyTable[i].paint (g);
   }

   public Dimension getPreferredSize() {
      return new Dimension (getWidth(), getHeight());
   }

   public int getWidth ()
   {
      int width = lowWidth;

      char[] layout = keyLayout.toCharArray();

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
      if (!keymapAssigned)
         return;

      int tone = keymap.getTone (event.getKeyCode());

      if (tone >= 0) {
         tone += transpose;

         if (tone > 255 || tone < 0)
            return;

         switch (event.getID()) {
            case KeyEvent.KEY_PRESSED:
               noteOn (tone, MASK_KEYBOARD);
               break;

            case KeyEvent.KEY_RELEASED:
               noteOff (tone, MASK_KEYBOARD);
               break;
         }
      }
      else if (event.getID() == KeyEvent.KEY_PRESSED) {
         switch (tone) {
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

            case Keymap.KEY_VOLUME_UP:
            case Keymap.KEY_VOLUME_DOWN:
            case Keymap.KEY_VOLUME_DEFAULT:
               // TODO: write me!

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
               transpose = TRANSPOSE_DEFAULT;
               break;

            case Keymap.KEY_ALL_NOTES_OFF:
               allNotesOff (255);
               break;
         }
      }
   }

   public void pedalOn ()
   { 
      pedal = true;

      for (int key = 0; key < 256; key++)
         if (keyMask[key] > 0)
            pedalList[key] = true;
   }

   public void pedalOff ()
   {
      pedal = false;

      if (visualAssigned)
         visual.allNotesOff ();

      for (int key = 0; key < 256; key++) {
         if (pedalList[key] == true) {
            if (keyMask[key] == 0)
               mc[channel].noteOff (key, 600);
            else if (visualAssigned)
               visual.noteOn (key);

            pedalList[key] = false;
         }
      }
   }

   protected void processMouseEvent (MouseEvent event)
   {
      switch (event.getID()) {
         case MouseEvent.MOUSE_ENTERED:
            break;

         case MouseEvent.MOUSE_RELEASED:
         case MouseEvent.MOUSE_EXITED:
            setMouseDown (MOUSE_RELEASED);
            break;

         case MouseEvent.MOUSE_PRESSED:
            for (int i = 0; i < keys; i++) {
               if (keyTable[i].mouseInBounds (event.getX(), event.getY())) {
                  setMouseDown (i + lowestNote);
                  return;
               }
            }
            break;
      }
   }

   protected void processMouseMotionEvent (MouseEvent event)
   {
      if (mouseDown != MOUSE_RELEASED) {
         for (int i = 0; i < keys; i++) {
            if (keyTable[i].mouseInBounds (event.getX(), event.getY())) {
               setMouseDown (i + lowestNote);
               return;
            }
         }
      }
      setMouseDown (MOUSE_RELEASED);
   }

   private void setMouseDown (int key)
   {
      if (key == mouseDown)
         return;

      if (mouseDown != MOUSE_RELEASED)
         noteOff (mouseDown, MASK_MOUSE);
      if (key != MOUSE_RELEASED)
         noteOn (key, MASK_MOUSE);

      mouseDown = key;
   }

   void noteOn (int key, int mask)
   {
      noteOn (key, mask, true);
   }

   void noteOff (int key, int mask)
   {
      noteOff (key, mask, true);
   }

   void noteOn (int key, int mask, boolean sound)
   {
      if (keyMask[key] == 0) {
         try {
            keyTable[key - lowestNote].setPressed (true);
            keyTable[key - lowestNote].paint (getGraphics());
         } catch (Exception e) { }

         if (sound)
            mc[channel].noteOn (key, 600);

         if (visualAssigned)
            visual.noteOn (key);

         if (pedal)
            pedalList[key] = true;
      }
      keyMask[key] |= mask;
   }

   void noteOff (int key, int mask, boolean sound)
   {
      keyMask[key] &= ~mask;

      if (keyMask[key] == 0) {
         try {
            keyTable[key - lowestNote].setPressed (false);
            keyTable[key - lowestNote].paint (getGraphics());
         } catch (Exception e) {
            System.out.println (e);
         }

         if (!pedal) {
            if (visualAssigned)
               visual.noteOff (key);

            if (sound)
               mc[channel].noteOff (key, 600);
         }
      }
   }

   void allNotesOff (int mask)
   {
      pedalOff ();

      for (int key = 0; key < 256; key++) {
         if ((keyMask[key] & mask) > 0) {
            try {
               keyTable[key - lowestNote].setPressed (false);
               keyTable[key - lowestNote].paint (getGraphics());
            } catch (Exception e) {
               System.out.println (e);
            }
            noteOff (key, mask);
         }
      }
   }

   public void setLayout (String keyLayout, int keys, int lowestNote)
                          throws InvalidKeyLayoutException
   {
      // TODO: throw exceptions for # of keys and lowest note

      if (!validKeyLayout (keyLayout))
         throw new InvalidKeyLayoutException ();

      this.keyLayout = keyLayout;
      this.keys = keys;
      this.lowestNote = lowestNote;

      buildKeyTables ();
      enableEvents (AWTEvent.MOUSE_MOTION_EVENT_MASK |
                    AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK |
                    AWTEvent.FOCUS_EVENT_MASK);

      setSize (getPreferredSize ());

      if (getParent() != null)
         getParent().doLayout ();
   }

   public void assignKeymap (Keymap keymap)
   {
      this.keymap = keymap;
      keymapAssigned = true;
   }

   public void assignVisual (Visual visual)
   {
      this.visual = visual;
      visualAssigned = true;
   }

   public void setMidi (int channel, int instrument)
   {
      this.channel = channel;
      this.instrument = instrument;

      try {
         mc[channel].programChange (instrument);
         //synth.loadInstrument (instr[this.instrument]);
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

      return true;
   }

   public boolean isFocusable ()
   {
      return true;
   }

   public void ProcessFocusEvent (FocusEvent e)
   {
      if (e.getID() == FocusEvent.FOCUS_LOST)
         allNotesOff (MASK_MOUSE | MASK_KEYBOARD);
   }
}
