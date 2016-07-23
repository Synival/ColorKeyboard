import javax.swing.*;
import java.util.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.RenderingHints;

public class Visual extends JPanel
{
   // Static definitions.
   public static final int DEFAULT_WIDTH  = 100;
   public static final int DEFAULT_HEIGHT = 100;

   // Internal variables.
   private int width;
   private int height;

   // Polygon tracking.
   private float[]      polyOn  = new float[Piano.TONE_RANGE];
   private float[]      polyDir = new float[Piano.TONE_RANGE];
   private VisualPoly[] poly = new VisualPoly[Piano.TONE_RANGE];
   private VisualPoly   mainPoly;

   public void run (float t)
   {
      int count = 0;

      // Update polygon intensities.
      for (int i = 0; i < Piano.TONE_RANGE; i++) {
         // Skip polygons that don't exist.
         if (poly[i] == null)
            continue;

         // Modify intensity based on tone (higher patch = faster).
         float speed = (float) Math.pow (2.00, (double) (i - 32) / 64.00);
         polyOn[i] += polyDir[i] * t * speed;

         // If we were increasing intensity, drop once we hit the peak.
         if (polyDir[i] > 0.00f) {
            if (polyOn[i] >= 1.00f) {
               polyOn[i] = 1.00f;
               polyDir[i] = -0.125f;
            }
         }
         // If we were decreasing intensity, delete polygons that hit zero.
         else if (polyDir[i] < 0.00f && polyOn[i] <= 0.00f) {
            poly[i] = null;
            continue;
         }

         // Move polygons that are active.
         poly[i].setBrightness (polyOn[i]);
         poly[i].polyMove ();
         count++;
      }

      // Update the main polygon if there are other polygons to draw.
      if (count > 0) {
         rebuildMainPoly ();
         mainPoly.polyMove ();
      }
      repaint ();
   }

   public Visual ()
   {
      this (DEFAULT_WIDTH, DEFAULT_HEIGHT);
   }

   public Visual (int width, int height)
   {
      // Set internal variables.
      this.width = width;
      this.height = height;

      // Initialze our component's size and background.
      setSize (getPreferredSize ());
      setOpaque (true);
      setBackground (Color.BLACK);

      // We always have a single, main polygon to represent all keys.
      mainPoly = new VisualPoly (this, Color.black);
   }

   public void paintComponent (Graphics g)
   {
      // Set background.
      super.paintComponent (g);
      setBackground (averageColor().darker());

      // Draw all polygons.
      for (int i = 0; i < Piano.TONE_RANGE; i++)
         if (polyOn[i] > 0)
            poly[i].paintComponent (g);
      mainPoly.paintComponent (g);
   }

   public int getWidth ()
   {
      return width;
   }

   public int getHeight ()
   {
      return height;
   }

   public Dimension getPreferredSize ()
   {
      return new Dimension (getWidth(), getHeight());
   }

   public float noteToHue (int note)
   {
      // Translate note to a hue clamped from (0 .. < 1)
      int hue = note;
      if (hue % 2 == 1)
         hue += 6;
      hue %= 12;
      return (float) hue / 12f;
   }

   public void noteOn (int note)
   {
      // Create a new polygon with the appropriate color.
      Color color = Color.getHSBColor (noteToHue(note), 1f, 0.4f);
      poly[note] = new VisualPoly (this, color);

      // Fade in rapidly from (at least) 0% opacity.
      polyDir[note] = 15.00f;
      if (polyOn[note] < 0.00f)
         polyOn[note] = 0.00f;
   }

   public void noteOff (int note)
   {
      // Gradually fade out.
      polyDir[note] = -2.00f;
   }

   public void allNotesOff ()
   {
      for (int i = 0; i < Piano.TONE_RANGE; i++)
         noteOff (i);
   }

   public void rebuildMainPoly ()
   {
      // Use a color brighter than our background and other polygons.
      mainPoly.setColor (averageColor().brighter());
   }

   public Color averageColor ()
   {
      float t, r, b = 0f, extra = 0f;
      float x = 0f, y = 0f;
      Color color;

      // Determine (x, y) position on a color wheel using all polygons.
      for (int i = 0; i < Piano.TONE_RANGE; i++) {
         if (polyOn[i] <= 0.00)
            continue;

         // Get polygon color and convert it to HSB.
         float[] hsb = new float[3];
         color = poly[i].getColor ();
         Color.RGBtoHSB (color.getRed(), color.getGreen(),
                         color.getBlue(), hsb);

         // Convert HSB to XYB and move our color wheel.
         x += Math.cos (hsb[0] * Math.PI * 2f) * (float) polyOn[i];
         y += Math.sin (hsb[0] * Math.PI * 2f) * (float) polyOn[i];
         b += polyOn[i];
      }
      if (b <= 0.00)
         return Color.black;

      // Divide (x, y) by magnitude.
      x /= b;
      y /= b;

      // t(theta) = hue,
      // r        = radius.
      t = (float) (Math.atan2 (y, x) / Math.PI / 2f);
      if (t > -0.01f && t < 0.01f)
         t = 0;
      r = (float) (Math.sqrt (x * x + y * y));

      // If the intensity is greater than 1, scale up to 1.00 logarithmically.
      if (b > 1f)
         b = (((b * 2f) - 1f) / (b * 2f));
      else
         b /= 2f;

      // We have our HSB value - return an RGB color.
      return Color.getHSBColor (t, r, (float) b);
   }
}
