import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

@SuppressWarnings("serial")
public class RoboGame extends JFrame {

	private WorldComponent worldComp = new WorldComponent();
	private File code1, code2;

	public static boolean debugDisplay = true;

	public RoboGame() {
		super("Robots");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);

		add(worldComp, BorderLayout.CENTER);

		createMenu();
		pack();

		setLocationRelativeTo(null);

		setVisible(true);
	}

	private void createMenu() {
		JMenuBar menu = new JMenuBar();

		final JMenu loadMenu = new JMenu("Load Program");
		final JMenu debugMenu = new JMenu("Debug ");
		final JMenuItem load1 = new JMenuItem("Robot 1");
		final JMenuItem load2 = new JMenuItem("Robot 2");
		final JMenuItem start = new JMenuItem("Start");
		final JMenuItem reset = new JMenuItem("Reset");
		final JMenuItem debugOn = new JMenuItem("On");
		final JMenuItem debugOff = new JMenuItem("Off");
		JMenuItem quit = new JMenuItem("Quit");

		menu.add(loadMenu);
		loadMenu.add(load1);
		loadMenu.add(load2);
		menu.add(start);
		menu.add(reset);
		menu.add(debugMenu);
		debugMenu.add(debugOn);
		debugMenu.add(debugOff);
		menu.add(quit);
		setJMenuBar(menu);

		// Add listeners to menu items.

		load1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				code1 = getCodeFile();
				if (code1 != null) {
					worldComp.loadRobotProgram(1, code1);
					worldComp.repaint();
				}
			}
		});

		load2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				code2 = getCodeFile();
				if (code2 != null) {
					worldComp.loadRobotProgram(2, code2);
					worldComp.repaint();
				}
			}
		});

		start.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				load1.setEnabled(false);
				load2.setEnabled(false);
				start.setEnabled(false);
				worldComp.start();
			}
		});
		reset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				worldComp.reset();
				if (code1 != null) {
					worldComp.loadRobotProgram(1, code1);
				}
				if (code2 != null) {
					worldComp.loadRobotProgram(2, code2);
				}
				worldComp.repaint();
				load1.setEnabled(true);
				load2.setEnabled(true);
				start.setEnabled(true);
			}
		});

		debugOn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				debugDisplay = true;
			}
		});
		debugOff.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				debugDisplay = false;
			}
		});

		quit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		/*
		 * } catch (FileNotFoundException e) {e.printStackTrace();} JMenuItem
		 * startSimple = new JMenuItem("Simple Interpreter"); JMenuItem
		 * startIntermediate = new JMenuItem("Intermediate Interpreter");
		 * JMenuItem startAdvanced = new JMenuItem("Advanced Interpreter");
		 * start.add(startSimple); start.add(startIntermediate);
		 * start.add(startAdvanced); startSimple.addActionListener(new
		 * ActionListener() {@Override public void actionPerformed(ActionEvent
		 * e) { load1.setEnabled(false); load2.setEnabled(false);
		 * start.setEnabled(false); worldComp.start(World.INTERPRETER_SIMPLE); }
		 * });
		 * 
		 * startIntermediate.addActionListener(new ActionListener() {@Override
		 * public void actionPerformed(ActionEvent e) { load1.setEnabled(false);
		 * load2.setEnabled(false); start.setEnabled(false);
		 * worldComp.start(World.INTERPRETER_INTERMEDIATE); } });
		 * startAdvanced.addActionListener(new ActionListener() {@Override
		 * public void actionPerformed(ActionEvent e) { load1.setEnabled(false);
		 * load2.setEnabled(false); start.setEnabled(false);
		 * worldComp.start(World.INTERPRETER_ADVANCED); } });
		 */
	}

	public File getCodeFile() {
		JFileChooser chooser = new JFileChooser(".");// System.getProperty("user.dir"));
		int res = chooser.showOpenDialog(this);
		if (res == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile();
		}
		return null;
	}

	/**
	 * This is the entry point into the program.
	 */
	public static void main(String[] args) {
		new RoboGame();
	}
}
