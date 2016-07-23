import java.awt.Color;
import java.awt.Graphics;

class VisualPoly
{
   // Keep track of parent visualizer.
   Visual visualizer;

   // Positional data.
   int points = (int) (Math.random() * 6) + 3;
   double[] xCoord, yCoord;
   double[] xVel, yVel;

   // Color information.
   Color color;
   float[] hsb = new float[3];
   float brightness = 1.00f;

   void setBrightness (float b)
   {
      this.brightness = b;
   }

   void polyMove ()
   {
      // Get boundaries.
      int width  = visualizer.getWidth(),
          height = visualizer.getHeight();

      // Move each point individually.
      for (int i = 0; i < points; i++) {
         // Move based on value/brightness.
         xCoord[i] += xVel[i] * hsb[2] * brightness;
         yCoord[i] += yVel[i] * hsb[2] * brightness;

         // X coordinate bounce.
         if (xCoord[i] < 0f) {
            xCoord[i] = -xCoord[i];
            xVel[i] = -xVel[i];
         }
         else if (xCoord[i] >= width) {
            xCoord[i] = (width * 2) - xCoord[i];
            xVel[i] = -xVel[i];
         }

         // Y coordinate bounce.
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
      // Keep track of visualizer.
      this.visualizer = v;

      // Assign coordinate array.
      this.xCoord = new double[points];
      this.yCoord = new double[points];
      this.xVel   = new double[points];
      this.yVel   = new double[points];

      // Initialize coordinates.
      int width  = visualizer.getWidth ();
      int height = visualizer.getHeight ();
      for (int i = 0; i < points; i++) {
         xCoord[i] = Math.random() * width;
         yCoord[i] = Math.random() * height;
         xVel[i] = (Math.random() - 0.5f) * width / 20f;
         yVel[i] = (Math.random() - 0.5f) * height / 20f;
      }

      // Initialize color.
      setColor (color);
   }

   public void paintComponent (Graphics graphics)
   {
      // Build a new color based on brightness.
      Color newColor;
      if (brightness == 1.00)
         newColor = color;
      else if (brightness == 0.00)
         newColor = Color.black;
      else {
         int r = (int) ((float) color.getRed()   * brightness),
             g = (int) ((float) color.getGreen() * brightness),
             b = (int) ((float) color.getBlue()  * brightness);
         r = Math.max (0, Math.min (255, r));
         g = Math.max (0, Math.min (255, g));
         b = Math.max (0, Math.min (255, b));
         newColor = new Color (r, g, b);
      }

      // Draw all lines in a loop.
      graphics.setColor (newColor);
      for (int i = 0; i < points; i++)
         graphics.drawLine ((int) xCoord[i], (int) yCoord[i],
                            (int) xCoord[(i + 1) % points],
                            (int) yCoord[(i + 1) % points]);
   }

   public Color getColor ()
   {
      return color;
   }

   public void setColor (Color color)
   {
      // Assign color.
      this.color = color;

      // Get HSB data.
      Color.RGBtoHSB (color.getRed(), color.getGreen(), color.getBlue(),
                      this.hsb);
   }
}
