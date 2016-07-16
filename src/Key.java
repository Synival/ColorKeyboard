import java.awt.*;

public class Key
{
   private int highOffset, highWidth, highHeight;
   private int lowWidth, lowHeight;
   private Color color, onColor;
   private boolean keyOn;

   private int x, y;

   public Key (int x, int y, int highOffset, int highWidth, int highHeight,
               int lowWidth, int lowHeight, Color color, Color onColor)
   {
      this.x = x;
      this.y = y;

      this.highOffset = highOffset;
      this.highWidth = highWidth;
      this.highHeight = highHeight;
      this.lowWidth = lowWidth;
      this.lowHeight = lowHeight;

      this.color = color;
      this.onColor = onColor;

      keyOn = false;
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
      if (mousex >= x && mousex < x + lowWidth &&
          mousey >= y + highHeight && mousey < y + highHeight + lowHeight)
         return true;
      else if (mousex >= x + highOffset && mousex < x + highOffset +
               highWidth && mousey >= y && mousey < y + highHeight + lowHeight)
         return true;

      return false;
   }

   public void paint (Graphics g)
   {
      Color keyColor, borderColor;

      if (keyOn) {
         keyColor = onColor;
         borderColor = Color.white;
      }
      else {
         keyColor = color;
         borderColor = Color.black;
      }

      g.setColor (borderColor);
      g.drawRect (x + highOffset, y, highWidth - 1, highHeight - 1);
      g.drawRect (x, y + highHeight, lowWidth - 1, lowHeight - 1);

      g.setColor (keyColor);
      g.fillRect (x + highOffset + 1, y + 1, highWidth - 2, highHeight - 2);
      g.fillRect (x + 1, y + highHeight + 1, lowWidth - 2, lowHeight - 2);

      if (lowHeight != 0)
         g.drawRect (x + highOffset + 1, y + highHeight - 1, highWidth - 3, 1);
   }
}
