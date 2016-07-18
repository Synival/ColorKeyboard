import java.util.*;
import javax.sound.midi.*;

class Metronome
{
   private int channel;
   private int note;
   private int instrument;

   private Instrument[] instr;
   private MidiChannel[] mc;
   private Timer timer = null;
   private Synthesizer synth;

   class MetronomeTask extends TimerTask
   {
      boolean noteOn = false;
      public void run ()
      {
         // Toggle note state.
         if (noteOn) {
            mc[channel].noteOff (note);
            noteOn = false;
         }
         else {
            mc[channel].noteOn (note, 600);
            noteOn = true;
         }
      }
   }

   public Metronome ()
   {
      // Initialize with the metronome off (BPM <= 0).
      this (0);
   }

   public Metronome (int bpm)
   {
      // Use channel 9 (percussion) and use a nice clicking sound.
      this (bpm, 9, 60, 0);
   }

   public Metronome (int bpm, int channel, int note, int instrument)
   {
      // Set internal variables.
      this.instrument = instrument;
      this.note = note;
      this.channel = channel;

      // Initialize a MIDI synthesizer that belongs to our metronome.
      try {
         synth = MidiSystem.getSynthesizer ();
         synth.open ();
         mc = synth.getChannels ();
         instr = synth.getDefaultSoundbank().getInstruments();
         synth.loadInstrument(instr[this.instrument]);
      }
      catch (Exception e) {
         System.out.println ("Metronome(): " + e);
      }

      // Turn our metronome on if a BPM was supplied.
      if (bpm > 0)
         enable (bpm);
   }

   public void enable (int bpm)
   {
      // Cap our metronome to reasonable values.
      if (bpm < 1)
         bpm = 1;
      else if (bpm > 600)
         bpm = 600;

      // Turn our timer off.
      if (isEnabled ())
         disable ();

      // Start a new timer.  Tick twice for every metronome click -
      // one to turn the note on, another to turn the note off.
      timer = new Timer();
      timer.scheduleAtFixedRate (new MetronomeTask (), 0, 30000l / (long) bpm);
   }

   public void disable ()
   {
      // If there's a timer, get rid of it.
      if (timer != null) {
         timer.cancel ();
         timer = null;
      }
   }

   public boolean isEnabled ()
   {
      // Return 'true' if there's a timer.
      if (timer != null)
         return true;
      else
         return false;
   }
}
