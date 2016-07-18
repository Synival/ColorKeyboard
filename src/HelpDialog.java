import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

public class HelpDialog extends JDialog
{
   public HelpDialog (Frame parent, String text)
   {
      super (parent, "Help Me!", true);
      setLayout (new FlowLayout());
      setResizable (false);

      // Create our text.
      JTextArea textarea = new JTextArea (text, 12, 50);
      textarea.setWrapStyleWord (true);
      textarea.setLineWrap (true);
      textarea.setEditable (false);

      // Let it scroll.
      JScrollPane scroll = new JScrollPane (textarea,
         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

      // Add our scrollable text to the window.
      this.add (scroll);
   }
}
