import javax.swing.*;
import java.awt.Graphics;
import java.awt.Color;

// A single key on the keyboard.  Mostly drawing routines.
public class Key
{
   // Coordinates for drawing.  Divided into 'high' and 'low' sections for
   // lower-tier keys that are cut away from upper-tier keys.
   private int x, y;
   private int highWidth, highHeight, highOffset;
   private int lowWidth,  lowHeight;

   // Colors for off/on.
   private Color color, onColor;

   // Mutable values.
   private boolean keyOn = false;

   public Key (int x, int y, int highOffset, int highWidth, int highHeight,
               int lowWidth, int lowHeight, Color color, Color onColor)
   {
      // Assign internal data.
      this.x          = x;
      this.y          = y;
      this.highOffset = highOffset;
      this.highWidth  = highWidth;
      this.highHeight = highHeight;
      this.lowWidth   = lowWidth;
      this.lowHeight  = lowHeight;
      this.color      = color;
      this.onColor    = onColor;
   }

   public boolean isPressed ()
   {
      return keyOn;
   }

   public void setPressed (boolean pressed)
   {
      keyOn = pressed;
   }

   public boolean mouseInBounds (int mousex, int mousey)
   {
      // Is the mouse over the lower section of the key?
      if (mousex >= x              && mousex < x + lowWidth &&
          mousey >= y + highHeight && mousey < y + highHeight + lowHeight)
         return true;

      // What about the higher section?
      if (mousex >= x + highOffset && mousex < x + highOffset + highWidth &&
          mousey >= y              && mousey < y + highHeight + lowHeight)
         return true;

      // Checks failed; we're not over the key.
      return false;
   }

   public void paintComponent (Graphics g)
   {
      // Determine colors.
      Color keyColor, borderColor;
      if (keyOn) {
         keyColor = onColor;
         borderColor = Color.white;
      }
      else {
         keyColor = color;
         borderColor = Color.black;
      }

      // Draw the outer part of the key...
      g.setColor (borderColor);
      g.drawRect (x + highOffset, y, highWidth - 1, highHeight - 1);
      g.drawRect (x, y + highHeight, lowWidth - 1, lowHeight - 1);

      // ...then the inner part on top of it.
      g.setColor (keyColor);
      g.fillRect (x + highOffset + 1, y + 1, highWidth - 2, highHeight - 2);
      g.fillRect (x + 1, y + highHeight + 1, lowWidth - 2, lowHeight - 2);

      // If there's both a high and low part, draw a 1-pixel-tall line
      // between the two.
      if (lowHeight != 0)
         g.drawRect (x + highOffset + 1, y + highHeight - 1, highWidth - 3, 1);
   }
}
