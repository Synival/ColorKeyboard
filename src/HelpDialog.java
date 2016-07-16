import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

public class HelpDialog extends Dialog implements ActionListener
{
   public HelpDialog (Frame parent, String text)
   {
      super (parent, "Help Me!", true);

      this.setSize (500, 200);
      this.setResizable (false);
      this.setLayout (new FlowLayout());

      TextArea a = new TextArea (text, 6, 50,
                                 TextArea.SCROLLBARS_VERTICAL_ONLY);
      Button b = new Button ("OK");

      a.setEditable (false);
      b.addActionListener (this);

      this.add (a);
      this.add (new HorizDivider ());
      this.add (b);

      Dimension screenSize = this.getToolkit().getScreenSize();
      Dimension size = this.getSize();

      setLocation ((screenSize.width  - size.width) / 2,
                   (screenSize.height - size.height) / 2);
   }

   public void actionPerformed (ActionEvent e)
   {
      this.hide ();
      this.dispose ();
   }
}
