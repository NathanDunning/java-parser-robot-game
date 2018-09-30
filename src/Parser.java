import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.*;
import javax.swing.JFileChooser;

/**
 * The parser and interpreter. The top level parse function, a main method for
 * testing, and several utility methods are provided. You need to implement
 * parseProgram and all the rest of the parser.
 */
public class Parser {

	/**
	 * Top level parse method, called by the World
	 */
	static RobotProgramNode parseFile(File code) {
		Scanner scan = null;
		try {
			scan = new Scanner(code);

			// the only time tokens can be next to each other is
			// when one of them is one of (){},;
			scan.useDelimiter("\\s+|(?=[{}(),;])|(?<=[{}(),;])");

			RobotProgramNode n = parseProgram(scan); // You need to implement this!!!

			scan.close();
			return n;
		} catch (FileNotFoundException e) {
			System.out.println("Robot program source file not found");
		} catch (ParserFailureException e) {
			System.out.println("Parser error:");
			System.out.println(e.getMessage());
			scan.close();
		}
		return null;
	}

	/** For testing the parser without requiring the world */

	public static void main(String[] args) {
		if (args.length > 0) {
			for (String arg : args) {
				File f = new File(arg);
				if (f.exists()) {
					System.out.println("Parsing '" + f + "'");
					RobotProgramNode prog = parseFile(f);
					System.out.println("Parsing completed ");
					if (prog != null) {
						System.out.println("================\nProgram:");
						System.out.println(prog);
					}
					System.out.println("=================");
				} else {
					System.out.println("Can't find file '" + f + "'");
				}
			}
		} else {
			while (true) {
				JFileChooser chooser = new JFileChooser(".");// System.getProperty("user.dir"));
				int res = chooser.showOpenDialog(null);
				if (res != JFileChooser.APPROVE_OPTION) {
					break;
				}
				RobotProgramNode prog = parseFile(chooser.getSelectedFile());
				System.out.println("Parsing completed");
				if (prog != null) {
					System.out.println("Program: \n" + prog);
				}
				System.out.println("=================");
			}
		}
		System.out.println("Done");
	}

	// Useful Patterns

	static Pattern NUMPAT = Pattern.compile("-?\\d+"); // ("-?(0|[1-9][0-9]*)");
	static Pattern OPENPAREN = Pattern.compile("\\(");
	static Pattern CLOSEPAREN = Pattern.compile("\\)");
	static Pattern OPENBRACE = Pattern.compile("\\{");
	static Pattern CLOSEBRACE = Pattern.compile("\\}");

	/**
	 * PROG ::= STMT+
	 */
	static RobotProgramNode parseProgram(Scanner s) {
		PROGNode ProgramNode = new PROGNode();

		while (s.hasNext()) {
			ProgramNode.children.add(parseStatement(s));
		}
		return ProgramNode;
	}

	/**
	 * STMT ::= ACT ";" | LOOP
	 */
	static RobotProgramNode parseStatement(Scanner s) {
		STMTNode StatementNode;

		if (s.hasNext("loop")) {
			StatementNode = new STMTNode(parseLoop(s));
		} else if (s.hasNext("if")) {
			StatementNode = new STMTNode(parseIf(s));
		} else if (s.hasNext("while")) {
			StatementNode = new STMTNode(parseWhile(s));
		} else {
			StatementNode = new STMTNode(parseAction(s));
			require(";", "Missing ';'", s);
		}

		return StatementNode;

	}

	/**
	 * ACT ::= "move" | "turnL" | "turnR" | "takeFuel" | "wait"
	 */
	static RobotProgramNode parseAction(Scanner s) {
		String next = s.next();

		if (next.equals("move")) {
			if (s.hasNext("(")) {
				require(OPENPAREN, "Missing '('", s);
				EXPNode param = (EXPNode) parseExpression(s);
				require(CLOSEPAREN, "Missing ')'", s);
				return new ACTNode(new MoveNode(param));
			} else {
				return new ACTNode(new MoveNode());
			}
		} else if (next.equals("wait")) {
			if (s.hasNext("(")) {
				require(OPENPAREN, "Missing '('", s);
				EXPNode param = (EXPNode) parseExpression(s);
				require(CLOSEPAREN, "Missing ')'", s);
				return new ACTNode(new WaitNode(param));
			} else {
				return new ACTNode(new WaitNode());
			}
		}
		else if (next.equals("turnL")) {
			return new ACTNode(new TurnLNode());
		} else if (next.equals("turnR")) {
			return new ACTNode(new TurnRNode());
		} else if (next.equals("turnAround")) {
			return new ACTNode(new TurnAroundNode());
		} else if (next.equals("shieldOn")) {
			return new ACTNode(new ShieldOnNode());
		} else if (next.equals("shieldOff")) {
			return new ACTNode(new ShieldOffNode());
		} else if (next.equals("takeFuel")) {
			return new ACTNode(new TakeFuelNode());
		} else if (next.equals("wait")) {
			return new ACTNode(new WaitNode());
		} else {
			fail("No actions can be found", s);
			return null;
		}

	}

	/**
	 * LOOP ::= "loop" BLOCK
	 */
	static RobotProgramNode parseLoop(Scanner s) {
		LOOPNode LoopNode;

		require("loop", "Missing 'loop'", s);
		LoopNode = new LOOPNode(parseBlock(s));

		return LoopNode;
	}

	/**
	 * BLOCK ::= "{" STMT+ "}"
	 */
	static RobotProgramNode parseBlock(Scanner s) {
		BLOCKNode BlockNode = new BLOCKNode();
		require(OPENBRACE, "Missing '{'", s);

		if (s.hasNext(CLOSEBRACE)) {
			fail("Cannot have empty block", s);
		}

		while (!s.hasNext("}")) {
			BlockNode.children.add(parseStatement(s));
		}

		require(CLOSEBRACE, "Missing '}'", s);

		return BlockNode;
	}

	static RobotProgramNode parseIf(Scanner s) {
		require("if", "Missing 'if'", s);
		require(OPENPAREN, "Missing '('", s);
		CONDNode cond = (CONDNode) parseCondition(s);
		require(CLOSEPAREN, "Missing ')'", s);
		BLOCKNode block = (BLOCKNode) parseBlock(s);
		if (s.hasNext("else")) {
			return new IFNode(cond, block, (ELSENode) parseElse(s));
		} else {		
			return new IFNode(cond, block);
		}
	}
	
	static RobotProgramNode parseElse(Scanner s) {
		require("else", "Missing 'else'", s);
		require(OPENBRACE, "Missing '{'", s);
		BLOCKNode block = (BLOCKNode) parseBlock(s);
		require(CLOSEBRACE, "Missing '}'", s);
		return new ELSENode(block);
	}

	static RobotProgramNode parseWhile(Scanner s) {
		require("while", "Missing 'while'", s);
		require(OPENPAREN, "Missing '('", s);
		CONDNode cond = (CONDNode) parseCondition(s);
		require(CLOSEPAREN, "Missing ')'", s);
		BLOCKNode block = (BLOCKNode) parseBlock(s);

		return new WHILENode(cond, block);

	}

	static ConditionNode parseCondition(Scanner s) {
		if (s.hasNext("and")) {
			require("and", "Missing 'and'", s);
			require(OPENPAREN, "Missing '('", s);
			ConditionNode p1 = parseCondition(s);
			require(",", "Missing ','", s);
			ConditionNode p2 = parseCondition(s);
			require(CLOSEPAREN, "Missing ')'", s);
			return new CONDNode("and", p1, p2);
		}
		else if (s.hasNext("or")) {
			require("or", "Missing 'or'", s);
			require(OPENPAREN, "Missing '('", s);
			ConditionNode p1 = parseCondition(s);
			require(",", "Missing ','", s);
			ConditionNode p2 = parseCondition(s);
			require(CLOSEPAREN, "Missing ')'", s);
			return new CONDNode("or", p1, p2);
		}
		else if (s.hasNext("not")) {
			require("not", "Missing 'not'", s);
			require(OPENPAREN, "Missing '('", s);
			ConditionNode p1 = parseCondition(s);
			require(CLOSEPAREN, "Missing ')'", s);
			return new CONDNode("not", p1, null);
		}
		else {
			RELOPNode relop = (RELOPNode) parseRelop(s);
			require(OPENPAREN, "Missing '('", s);
			EXPNode expr1 = new EXPNode(parseExpression(s));
			require(",", "Missing ','", s);
			EXPNode expr2 = new EXPNode(parseExpression(s));
			require(CLOSEPAREN, "Missing ')'", s);
			return new CONDNode(relop, expr1, expr2);
		}

	}

	static SensorNode parseExpression(Scanner s) {
		EXPNode exp;

		if (s.hasNextInt()) {
			exp = new EXPNode(parseNumber(s));
		} 
		else if (s.hasNext("add") || s.hasNext("sub") || s.hasNext("mul") || s.hasNext("div")) {
			OPNode operation = (OPNode) parseOP(s);
			require(OPENPAREN, "Missing '('", s);
			EXPNode e1 = (EXPNode) parseExpression(s);
			require(",", "Missing ','", s);
			EXPNode e2 = (EXPNode) parseExpression(s);
			require(CLOSEPAREN, "Missing ')'", s);
			return new EXPNode(operation, e1, e2);
		}
		else {
			exp = new EXPNode(parseSensor(s));
		}

		return exp;
	}
	
	static SensorNode parseOP(Scanner s) {
		if (s.hasNext("add")) {
			return new OPNode("add");
		}
		else if (s.hasNext("sub")) {
			return new OPNode("sub");
		}
		else if (s.hasNext("mul")) {
			return new OPNode("mul");
		}
		else if (s.hasNext("div")) {
			return new OPNode("div");
		}
		return null;
	}

	static ConditionNode parseRelop(Scanner s) {
		String next = s.next();

		if (next.equals("lt")) {
			return new RELOPNode(new LTNode());
		} else if (next.equals("gt")) {
			return new RELOPNode(new GTNode());
		} else if (next.equals("eq")) {
			return new RELOPNode(new EQNode());
		} else {
			fail("Unable to find RELOP", s);
			return null;
		}
	}

	static SensorNode parseSensor(Scanner s) {
		String next = s.next();

		if (next.equals("fuelLeft")) {
			return new SENNode(new FuelLeftNode());
		} else if (next.equals("oppLR")) {
			return new SENNode(new OppLRNode());
		} else if (next.equals("oppFB")) {
			return new SENNode(new OppFBNode());
		} else if (next.equals("numBarrels")) {
			return new SENNode(new NumBarrelsNode());
		} else if (next.equals("barrelLR")) {
			return new SENNode(new BarrelLRNode());
		} else if (next.equals("barrelFB")) {
			return new SENNode(new BarrelFBNode());
		} else if (next.equals("wallDist")) {
			return new SENNode(new WallDistNode());
		} else {
			fail("Unable to find SEN", s);
			return null;
		}

	}

	static SensorNode parseNumber(Scanner s) {
		return new NUMNode(requireInt(NUMPAT, "Unable to find number", s));
	}

	// utility methods for the parser

	/**
	 * Report a failure in the parser.
	 */
	static void fail(String message, Scanner s) {
		String msg = message + "\n   @ ...";
		for (int i = 0; i < 5 && s.hasNext(); i++) {
			msg += " " + s.next();
		}
		throw new ParserFailureException(msg + "...");
	}

	/**
	 * Requires that the next token matches a pattern if it matches, it consumes and
	 * returns the token, if not, it throws an exception with an error message
	 */
	static String require(String p, String message, Scanner s) {
		if (s.hasNext(p)) {
			return s.next();
		}
		fail(message, s);
		return null;
	}

	static String require(Pattern p, String message, Scanner s) {
		if (s.hasNext(p)) {
			return s.next();
		}
		fail(message, s);
		return null;
	}

	/**
	 * Requires that the next token matches a pattern (which should only match a
	 * number) if it matches, it consumes and returns the token as an integer if
	 * not, it throws an exception with an error message
	 */
	static int requireInt(String p, String message, Scanner s) {
		if (s.hasNext(p) && s.hasNextInt()) {
			return s.nextInt();
		}
		fail(message, s);
		return -1;
	}

	static int requireInt(Pattern p, String message, Scanner s) {
		if (s.hasNext(p) && s.hasNextInt()) {
			return s.nextInt();
		}
		fail(message, s);
		return -1;
	}

	/**
	 * Checks whether the next token in the scanner matches the specified pattern,
	 * if so, consumes the token and return true. Otherwise returns false without
	 * consuming anything.
	 */
	static boolean checkFor(String p, Scanner s) {
		if (s.hasNext(p)) {
			s.next();
			return true;
		} else {
			return false;
		}
	}

	static boolean checkFor(Pattern p, Scanner s) {
		if (s.hasNext(p)) {
			s.next();
			return true;
		} else {
			return false;
		}
	}

}

// You could add the node classes here, as long as they are not declared public
// (or private)

class PROGNode implements RobotProgramNode {
	ArrayList<RobotProgramNode> children = new ArrayList<RobotProgramNode>();

	@Override
	public void execute(Robot robot) {
		for (RobotProgramNode r : children) {
			r.execute(robot);
		}
	}

	@Override
	public String toString() {
		String s = "";
		for (RobotProgramNode r : children) {
			s += r.toString();
		}
		return s;
	}

}

class STMTNode implements RobotProgramNode {
	final RobotProgramNode child;

	STMTNode(RobotProgramNode child) {
		this.child = child;
	}

	@Override
	public void execute(Robot robot) {
		child.execute(robot);
	}

	@Override
	public String toString() {
		return child.toString();
	}

}

class ACTNode implements RobotProgramNode {
	final RobotProgramNode child;

	ACTNode(RobotProgramNode child) {
		this.child = child;
	}

	@Override
	public void execute(Robot robot) {
		child.execute(robot);
	}

	@Override
	public String toString() {
		return child.toString();
	}

}

class LOOPNode implements RobotProgramNode {
	final BLOCKNode block;

	LOOPNode(RobotProgramNode block) {
		this.block = (BLOCKNode) block;
	}

	@Override
	public void execute(Robot robot) {
		block.execute(robot);
	}

	@Override
	public String toString() {
		return "\nloop{ \n" + this.block.toString() + "\n" + "}";
	}

}

class BLOCKNode implements RobotProgramNode {
	ArrayList<RobotProgramNode> children = new ArrayList<RobotProgramNode>();

	@Override
	public void execute(Robot robot) {
		for (RobotProgramNode r : children) {
			r.execute(robot);
		}
	}

	@Override
	public String toString() {
		String s = "";
		for (RobotProgramNode r : children) {
			s += r.toString();
		}
		return s;
	}

}

class IFNode implements RobotProgramNode {
	final CONDNode Condition;
	final BLOCKNode Block;
	final ELSENode Else;
	
	IFNode(CONDNode cond, RobotProgramNode block) {
		this.Condition = cond;
		this.Block = (BLOCKNode) block;
		this.Else = null;
	}


	IFNode(CONDNode cond, RobotProgramNode block, ELSENode Else) {
		this.Condition = cond;
		this.Block = (BLOCKNode) block;
		this.Else = Else;
	}

	@Override
	public void execute(Robot robot) {
		if (Condition.evaluate(robot)) {
			Block.execute(robot);
			if (Else.Block != null) {
				Else.execute(robot);
			}
		}
	}

	@Override
	public String toString() {
		return "if (" + Condition.toString() + ") " + "{\n\t" + Block.toString() + "\n} ";
	}

}

class ELSENode implements RobotProgramNode {
	final BLOCKNode Block;
	
	ELSENode(){
		this.Block = null;
	}
	
	ELSENode(BLOCKNode block){
		this.Block = block;
	}
	
	@Override
	public void execute(Robot robot) {
			Block.execute(robot);
	}
	
	@Override
	public String toString() {
		return "else " + "{\n\t" + Block.toString() + "\n} ";
	}
	
}

class WHILENode implements RobotProgramNode {
	final CONDNode Condition;
	final BLOCKNode Block;

	WHILENode(CONDNode Cond, RobotProgramNode Block) {
		this.Condition = Cond;
		this.Block = (BLOCKNode) Block;
	}

	@Override
	public void execute(Robot robot) {
		while (Condition.evaluate(robot)) {
			Block.execute(robot);
		}
	}

	@Override
	public String toString() {
		return "while (" + Condition.toString() + ") " + "{\n\t" + Block.toString() + "\n} ";
	}

}

class CONDNode implements ConditionNode {
	final RELOPNode Relop;
	final EXPNode expr1;
	final EXPNode expr2;
	final String cond;
	final ConditionNode condParam1;
	final ConditionNode condParam2;
	

	CONDNode(ConditionNode rel, EXPNode e1, EXPNode e2) {
		this.Relop = (RELOPNode) rel;
		this.expr1 = e1;
		this.expr2 = e2;
		this.cond = null;
		this.condParam1 = null;
		this.condParam2 = null;
	}
	
	CONDNode(String rel, ConditionNode e1, ConditionNode e2) {
		this.Relop = null;
		this.expr1 = null;
		this.expr2 = null;
		this.cond = rel;
		this.condParam1 = e1;
		this.condParam2 = e2;
	}

	@Override
	public boolean evaluate(Robot robot) {
		if (Relop.operation instanceof EQNode) {
			if (expr1.evaluate(robot) == expr2.evaluate(robot)) {
				return true;
			}
		} else if (Relop.operation instanceof GTNode) {
			if (expr1.evaluate(robot) > expr2.evaluate(robot)) {
				return true;
			}
		} else if (Relop.operation instanceof LTNode) {
			if (expr1.evaluate(robot) < expr2.evaluate(robot)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return Relop.toString() + "(" + expr1.toString() + ", " + expr2.toString() + ")";
	}

}

class RELOPNode implements ConditionNode {
	final ConditionNode operation;

	RELOPNode(ConditionNode op) {
		this.operation = op;
	}

	@Override
	public boolean evaluate(Robot robot) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString() {
		return operation.toString();
	}

}

class SENNode implements SensorNode {
	final SensorNode sensor;

	SENNode(SensorNode sensor) {
		this.sensor = sensor;
	}

	@Override
	public int evaluate(Robot robot) {
		return sensor.evaluate(robot);
	}

	@Override
	public String toString() {
		return sensor.toString();
	}

}

class EXPNode implements SensorNode {
	final OPNode operation;
	final SensorNode expr1;
	final SensorNode expr2;

	EXPNode(SensorNode e1) {
		this.operation = null;
		this.expr1 = e1;
		this.expr2 = null;
	}
	
	EXPNode(OPNode op, SensorNode e1, SensorNode e2){
		this.operation = op;
		this.expr1 = e1;
		this.expr2 = e2;
	}

	@Override
	public int evaluate(Robot robot) {
		if (this.operation == null) {
			return expr1.evaluate(robot);	
		}
		else {
			if (operation.operation.toString().equals("add")) {
				return expr1.evaluate(robot) + expr2.evaluate(robot);
			}
			else if (operation.operation.toString().equals("sub")) {
				return expr1.evaluate(robot) - expr2.evaluate(robot);
			}
			else if (operation.operation.toString().equals("mul")) {
				return expr1.evaluate(robot) * expr2.evaluate(robot);
			}
			else if (operation.operation.toString().equals("div")) {
				return expr1.evaluate(robot) / expr2.evaluate(robot);
			}
			else {return -1;}
		}
	}

	@Override
	public String toString() {
		
		if (this.operation == null) {
			return expr1.toString();
		}
		else {
			if (operation.operation.toString().equals("add")) {
				return "add (" + expr1.toString() + "," + expr2.toString() + ")";
			}
			else if (operation.operation.toString().equals("sub")) {
				return "sub (" + expr1.toString() + "," + expr2.toString() + ")";
			}
			else if (operation.operation.toString().equals("mul")) {
				return "mul (" + expr1.toString() + "," + expr2.toString() + ")";
			}
			else if (operation.operation.toString().equals("div")) {
				return "div (" + expr1.toString() + "," + expr2.toString() + ")";
			}
			else {return null;}
		}
		
	}

}

class NUMNode implements SensorNode {
	final int value;

	NUMNode(int val) {
		this.value = val;
	}

	@Override
	public int evaluate(Robot robot) {
		return value;
	}

	@Override
	public String toString() {
		return "" + value;
	}
}

class OPNode implements SensorNode {
	final String operation;
	
	OPNode(String s){
		this.operation = s;
	}
	
	@Override
	public int evaluate(Robot robot) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public String toString() {
		return this.operation;
	}
	
}

class LTNode implements ConditionNode {

	@Override
	public boolean evaluate(Robot robot) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString() {
		return "lt";
	}
}

class GTNode implements ConditionNode {

	@Override
	public boolean evaluate(Robot robot) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString() {
		return "gt";
	}
}

class EQNode implements ConditionNode {

	@Override
	public boolean evaluate(Robot robot) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString() {
		return "eq";
	}
}

class FuelLeftNode implements SensorNode {

	@Override
	public int evaluate(Robot robot) {
		return robot.getFuel();
	}

	@Override
	public String toString() {
		return "fuelLeft";
	}
}

class OppLRNode implements SensorNode {

	@Override
	public int evaluate(Robot robot) {
		return robot.getOpponentLR();
	}

	@Override
	public String toString() {
		return "oppLR";
	}
}

class OppFBNode implements SensorNode {

	@Override
	public int evaluate(Robot robot) {
		return robot.getOpponentFB();
	}

	@Override
	public String toString() {
		return "oppFB";
	}
}

class NumBarrelsNode implements SensorNode {

	@Override
	public int evaluate(Robot robot) {
		return robot.numBarrels();
	}

	@Override
	public String toString() {
		return "numBarrels";
	}
}

class BarrelLRNode implements SensorNode {

	@Override
	public int evaluate(Robot robot) {
		return robot.getClosestBarrelLR();
	}

	@Override
	public String toString() {
		return "barrelLR";
	}
}

class BarrelFBNode implements SensorNode {

	@Override
	public int evaluate(Robot robot) {
		return robot.getClosestBarrelFB();
	}

	@Override
	public String toString() {
		return "barrelFB";
	}
}

class WallDistNode implements SensorNode {

	@Override
	public int evaluate(Robot robot) {
		return robot.getDistanceToWall();
	}

	@Override
	public String toString() {
		return "wallDist";
	}
}

class MoveNode implements RobotProgramNode {
	final EXPNode expr;

	MoveNode() {
		expr = null;
	}

	MoveNode(EXPNode expr) {
		this.expr = expr;
	}

	@Override
	public void execute(Robot robot) {
		robot.move();

	}

	@Override
	public String toString() {
		return "move; ";
	}

}

class TurnLNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.turnLeft();

	}

	@Override
	public String toString() {
		return "turnL; ";

	}

}

class TurnRNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.turnRight();

	}

	@Override
	public String toString() {
		return "turnR; ";

	}

}

class TurnAroundNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.turnAround();

	}

	@Override
	public String toString() {
		return "turnAround; ";

	}
}

class ShieldOnNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.setShield(true);

	}

	@Override
	public String toString() {
		return "shieldOn; ";

	}
}

class ShieldOffNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.setShield(false);

	}

	@Override
	public String toString() {
		return "shieldOff; ";

	}
}

class TakeFuelNode implements RobotProgramNode {

	@Override
	public void execute(Robot robot) {
		robot.takeFuel();

	}

	@Override
	public String toString() {
		return "takeFuel; ";

	}

}

class WaitNode implements RobotProgramNode {
	final EXPNode expr;

	WaitNode() {
		expr = null;
	}

	WaitNode(EXPNode expr) {
		this.expr = expr;
	}
	
	@Override
	public void execute(Robot robot) {
		robot.idleWait();

	}

	@Override
	public String toString() {
		return "wait; ";

	}

}
