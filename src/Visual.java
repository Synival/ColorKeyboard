import javax.swing.*;
import java.util.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class Visual extends JPanel
{
   public static final int DEFAULT_WIDTH = 100;
   public static final int DEFAULT_HEIGHT = 100;

   private int width;
   private int height;

   private float[]      polyOn = new float[256];
   private VisualPoly[] poly = new VisualPoly[256];
   private VisualPoly   mainPoly;

   private boolean rebuild = false;

   public void run () {
      for (int i = 0; i < 256; i++)
         if (polyOn[i] > 0 && polyOn[i] < 1) {
            float drop = (float) i / 256f;
            polyOn[i] -= drop * Math.sqrt (drop);
         }

      if (rebuild)
         rebuildMainPoly ();

      for (int i = 0; i < 256; i++)
         if (polyOn[i] > 0) {
            poly[i].polyMove ();

            if  (polyOn[i] == 0) {
               rebuild = true;
               poly[i] = null;
            }
         }

      mainPoly.polyMove ();

      repaint ();
   }

   public Visual ()
   {
      this (DEFAULT_WIDTH, DEFAULT_HEIGHT);
   }

   public Visual (int width, int height)
   {
      this.width = width;
      this.height = height;

      mainPoly = new VisualPoly (this, Color.black);
      setSize (getPreferredSize ());
      setOpaque (true);
      setBackground (Color.BLACK);
   }

   public void paintComponent (Graphics g)
   {
      super.paintComponent (g);
      setBackground (averageColor().darker().darker());

      for (int i = 0; i < 256; i++)
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
      int hue = note;
      if (hue % 2 == 1)
         hue += 6;

      hue %= 12;

      return (float) hue / 12f;
   }

   public void noteOn (int note)
   {
      Color color = Color.getHSBColor (noteToHue(note), 1f, 0.4f);

      poly[note] = new VisualPoly (this, color);
      polyOn[note] = 1f;
   }

   public void noteOff (int note)
   {
      polyOn[note] = 0.99f;
   }

   public void allNotesOff ()
   {
      for (int i = 0; i < 256; i++)
         if (polyOn[i] == 1f)
            polyOn[i] = 0.99f;
   }

   public void rebuildMainPoly ()
   {
      mainPoly.setColor (averageColor());
   }

   public Color averageColor ()
   {
      float t, r, b = 0;
      float x = 0, y = 0;

      float polys = 0f;
      Color color;

      for (int i = 0; i < 256; i++)
         if (polyOn[i] > 0) {
            float[] hsb = new float[3];

            color = poly[i].getColor ();
            Color.RGBtoHSB (color.getRed(), color.getGreen(),
                            color.getBlue(), hsb);

            x += Math.cos (hsb[0] * Math.PI * 2f) * (float) polyOn[i];
            y += Math.sin (hsb[0] * Math.PI * 2f) * (float) polyOn[i];

            b += polyOn[i];
            polys += polyOn[i];
         }

      if (b > 1f)
         b = 1f;

      if (polys > 0) {
         x /= (float) polys;
         y /= (float) polys;

         t = (float) (Math.atan2 (y, x) / Math.PI / 2f) % 1.0f;
         r = (float) (Math.sqrt (x * x + y * y));

         if (t > -0.01f && t < 0.01f)
            t = 0;

         return (Color.getHSBColor (t, r, (float) b));
      }
      else {
         return Color.black;
      }
   }
}

class VisualPoly
{
   int points = (int) (Math.random() * 6) + 3;
   float sat;

   double[] xCoord, yCoord;
   double[] xVel, yVel;

   double width  = (float) Visual.DEFAULT_WIDTH;
   double height = (float) Visual.DEFAULT_HEIGHT;

   Color color;

   void polyMove ()
   {
      for (int i = 0; i < points; i++) {
         xCoord[i] += xVel[i] * sat;
         yCoord[i] += yVel[i] * sat;

         if (xCoord[i] < 0f) {
            xCoord[i] = -xCoord[i];
            xVel[i] = -xVel[i];
         }
         else if (xCoord[i] >= width) {
            xCoord[i] = (width * 2) - xCoord[i];
            xVel[i] = -xVel[i];
         }

         if (yCoord[i] < 0f) {
            yCoord[i] = -yCoord[i];
            yVel[i] = -yVel[i];
         }
         else if (yCoord[i] >= height) {
            yCoord[i] = (height * 2) - yCoord[i];
            yVel[i] = -yVel[i];
         }
      }
   }

   public VisualPoly (Visual v, Color color)
   {
      width  = v.getWidth();
      height = v.getHeight();

      setColor (color);

      xCoord = new double[points];
      yCoord = new double[points];
      xVel   = new double[points];
      yVel   = new double[points];

      for (int i = 0; i < points; i++) {
         xCoord[i] = Math.random() * width;
         yCoord[i] = Math.random() * height;

         xVel[i] = (Math.random() - 0.5f) * width / 20f;
         yVel[i] = (Math.random() - 0.5f) * height / 20f;
      }
   }

   public void paintComponent (Graphics g)
   {
      g.setColor (color);

      for (int i = 0; i < points; i++)
         g.drawLine ((int) xCoord[i], (int) yCoord[i],
                     (int) xCoord[(i + 1) % points],
                     (int) yCoord[(i + 1) % points]);
   }

   public Color getColor ()
   {
      return color;
   }

   public void setColor (Color color)
   {
      float[] hsb = new float[3];

      this.color = color;
      Color.RGBtoHSB (color.getRed(), color.getGreen(), color.getBlue(), hsb);
      sat = hsb[2];
   }
}
