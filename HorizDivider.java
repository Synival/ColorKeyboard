import java.awt.*;

class HorizDivider extends Canvas
{
   public HorizDivider ()
   {
      setSize (getPreferredSize ());
   }

   public Dimension getPreferredSize ()
   {
      if (getParent() == null)
         return new Dimension (100, 0);
      return new Dimension (getParent().getWidth(), 5);
   }
}
