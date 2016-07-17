import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

public class HelpDialog extends JDialog implements ActionListener
{
   public HelpDialog (JFrame parent, String text)
   {
      super (parent, "Help Me!", true);

      this.setSize (500, 200);
      this.setResizable (false);
      this.setLayout (new FlowLayout());

      JTextArea a = new JTextArea (text, 6, 50);
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
      this.setVisible (false);
      this.dispose ();
   }
}
