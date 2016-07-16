import java.util.*;
import javax.sound.midi.*;

class Metronome
{
   private int channel;
   private int note;
   private int instrument;

   private boolean timeron;

   private Instrument[] instr;
   private MidiChannel[] mc;
   private Timer timer;
   private Synthesizer synth;

   class MetronomeTask extends TimerTask
   {
      boolean noteon = false;

      public void run ()
      {
         if (noteon) {
            mc[channel].noteOff (note);
            noteon = false;
         }
         else {
            mc[channel].noteOn (note, 600);
            noteon = true;
         }
      }
   }

   public Metronome ()
   {
      this (0);
   }

   public Metronome (int bpm)
   {
      this (bpm, 9, 60, 0);
   }

   public Metronome (int bpm, int channel, int note, int instrument)
   {
      this.instrument = instrument;
      this.note = note;
      this.channel = channel;

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

      if (bpm != 0)
         enable (bpm);
   }

   public void enable (int bpm)
   {
      if (bpm < 1)
         bpm = 1;

      if (timeron)
         disable ();

      timer = new Timer();

      //try {
         MetronomeTask tick = new MetronomeTask();
         timer.scheduleAtFixedRate (tick, 0, 30000l / (long) bpm);
      //}
      //catch (Exception e) {
         //System.out.println ("Metronome.enable(): " + e);
      //}

      timeron = true;
   }

   public void disable ()
   {
      if (timeron) {
         timer.cancel ();
         timeron = false;
      }
   }

   public boolean isEnabled ()
   {
      return timeron;
   }
}
