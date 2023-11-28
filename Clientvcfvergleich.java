package readvcffiles;

/*
 *btncheck @ line 770+
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author Christoph Steisslinger christoph.steisslinger@writeme.com
 * @version 1.5.1
 * @since 15.08.2018
 */
public class Clientvcfvergleich {
	private static ArrayList<String> rsList = new ArrayList<String>();
	private static String alline;
	private static String POS_ID;
	private static String alt;
	private static String qual;
	private static String af;
	private static String chr;
	private static String myUrl1 = "jdbc:mysql://";
	private static String myUrl2 = "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
	private static String myUrl;
	private static String journal_number;
	private static ArrayList<String> snpidlist = new ArrayList<String>();
	private static ArrayList<String> chrlist = new ArrayList<String>();
	private static ArrayList<String> resultlist = new ArrayList<String>();
	private static ArrayList<String> notelist = new ArrayList<String>();
	private static ArrayList<String> printList = new ArrayList<String>();
	private static ArrayList<String> saveList = new ArrayList<String>();
	private static ArrayList<String> exoList = new ArrayList<String>();
	private static String username;
	private static String pw;
	private static String zugangspm = null;
	public static int okcounter = 0;
	public static int misscounter = 0;

	/**
	 * @param args
	 * @throws IOException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static void main(final String[] args)
			throws IOException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		final long timeStart = System.nanoTime();
		Connection conn = null;
		Statement st = null;

		ResultSet rs = null;
		Clientvcfvergleich.printList.add("#journal_number\tSNPID\tmatch_result\tPyro\tQuality\tChromosom\tExom\t"
				// + "SNP_to_journal_number\t"
				+ "Wildtyp\tStrang");
		if (("help".equals(args[0]) || "?".equals(args[0]) || "--help".equals(args[0]) || args[0].equals(null))
				&& (args[1] == null) && (args[2] == null)) {
			Clientvcfvergleich.printinfo();
		} else {
			final File f = new File(args[0]);
			final String resultpath = args[1];
			Clientvcfvergleich.zugangspm = args[2];
			if (f.exists() && !f.isDirectory()) {
				// do stuff
				try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {

					Clientvcfvergleich.getuserinfo();

					Clientvcfvergleich.myUrl = Clientvcfvergleich.myUrl1 + "10.10.89.80/inhousedatabase"
							+ Clientvcfvergleich.myUrl2;
					Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
					if (System.getProperty("os.name").startsWith("Windows")) {
						Clientvcfvergleich.setJournal_number(
								args[0].substring(args[0].lastIndexOf("\\") + 1, args[0].length()).replace("-", "/"));
					} else {
						Clientvcfvergleich.setJournal_number(
								args[0].substring(args[0].lastIndexOf("/") + 1, args[0].length()).replace("-", "/"));
					}
					Clientvcfvergleich.journal_number = Clientvcfvergleich.journal_number.substring(0,
							Clientvcfvergleich.journal_number.indexOf("."));
					String line = br.readLine();

					conn = DriverManager.getConnection(Clientvcfvergleich.myUrl, Clientvcfvergleich.username,
							Clientvcfvergleich.pw);
					final String query = "SELECT * FROM pyroergebnis";
					st = conn.createStatement();
					rs = st.executeQuery(query);
					final ResultSet rscopy = rs;

					while (line != null) {
						while (line.startsWith("#")) {
							line = br.readLine();

						}
						Clientvcfvergleich.splittopos(line);

						while (rscopy.next()) {
							// save to String-List
							Clientvcfvergleich.snpidlist.add(rs.getString("SNPID"));
							Clientvcfvergleich.notelist.add(rs.getString("note"));
							Clientvcfvergleich.resultlist.add(rs.getString("result"));
							Clientvcfvergleich.chrlist.add(rs.getString("chromosom"));
							Clientvcfvergleich.rsList.add(rs.getString("rsname"));

						} // end while(rs.next())

						Clientvcfvergleich.dostuff(conn);
						line = br.readLine();
					} // end while(line!=null)

					br.close();

					Clientvcfvergleich.printresult(rs, conn, resultpath);

					System.out.println("done");
					final long timeEnd = System.nanoTime();
					System.out.println("Verlaufszeit der Schleife: " + (timeEnd - timeStart) + " Nanosek.");
				} catch (final FileNotFoundException e) {
					System.out.println("vcf file not found ");
				} catch (final SQLException s) {
					System.out.println(s.getMessage());
				} catch (final IllegalAccessException i) {
					System.out.println(i.getMessage());
				} finally {
					// used to close stuff
					if (rs != null) {
						rs.close();
					}
					if (st != null) {
						st.close();
					}
					if (conn != null) {
						conn.close();
					}

				}
			} else {
				System.out.println("given path doesnt lead to a file");
			}
		}
	}

	/**
	 * prints results in txt and xlsx files + checks for null-matchs and matches
	 * with wildtypes
	 * 
	 * @param rscopysave
	 * @param conn
	 * @param rpath
	 * @throws IOException
	 * @throws SQLException
	 */
	private static void printresult(final ResultSet rscopysave, final Connection conn, final String rpath)
			throws IOException, SQLException {

		final String query = "SELECT * FROM sample_pyro " + " where journal_number='"
				+ Clientvcfvergleich.journal_number + "'";

		final Statement stp = conn.createStatement();
		final ResultSet rsp = stp.executeQuery(query);
		// get list from patient then write journalnumber from xxxx/xx to xxxx-xx

		final String backupjn = Clientvcfvergleich.journal_number;
		Clientvcfvergleich.journal_number = Clientvcfvergleich.journal_number.replaceFirst("/", "-");
		Clientvcfvergleich.journal_number.replaceAll("/", "-");

		FileWriter statText;
		// save in C:\\xxxx-xx.txt

		final String savepatq = rpath + "\\" + Clientvcfvergleich.journal_number + ".txt";
		final XSSFWorkbook workbook = new XSSFWorkbook();

		// Create a blank sheet
		final XSSFSheet spreadsheet = workbook.createSheet("patient");
		statText = new FileWriter(savepatq);
		final BufferedWriter w = new BufferedWriter(statText);
		w.write(Clientvcfvergleich.printList.get(0).toString());
		w.newLine();
		// is Query empty?
		if (!rsp.isBeforeFirst()) {
			System.out.println("No data");
			w.write("no SNPs found for " + Clientvcfvergleich.journal_number + "\t");
		}

		Clientvcfvergleich.journal_number = backupjn;
		int ki = 0;
		final Map<String, Object[]> empinfo = new TreeMap<String, Object[]>();
		empinfo.put("1", new Object[] { Clientvcfvergleich.journal_number, "SNPID", "match_result", "Pyro", "Quality",
				"Chromosom", "Exom", "Wildtyp", "Strang" });
		while (rsp.next()) {
			String pypy = "";
			int z = 0;
			while (z < Clientvcfvergleich.snpidlist.size()) {
				if (Clientvcfvergleich.snpidlist.get(z).toString().equals(rsp.getString("SNPID"))) {
					pypy = Clientvcfvergleich.resultlist.get(z).toString();
				}

				z++;
			}

			if (("MISS".equals(rsp.getString("match_result")) || (rsp.getString("match_result") == null))) {

				String wild = Clientvcfvergleich.getwildtype(rsp.getString("SNPID"));

				int pos = 0;
				for (int jj = 0; jj < Clientvcfvergleich.snpidlist.size(); jj++) {
					if (Clientvcfvergleich.snpidlist.get(jj).toString().equals(wild)) {
						pos = jj;
					}
				}
				if (Clientvcfvergleich.btncheck(Clientvcfvergleich.chrlist.get(pos))) {
					wild = Clientvcfvergleich.swapsinglebase(wild);

				}
				boolean check = true;
				for (int i = 0; i < Clientvcfvergleich.resultlist.size(); i++) {
					if (Clientvcfvergleich.checkresults(Clientvcfvergleich.resultlist.get(i).toString(), wild, wild)) {
						check = false;
						Clientvcfvergleich.savetoDB(conn, Clientvcfvergleich.snpidlist.get(i).toString(), "OK");
					}
				}
				if (check) {

					Clientvcfvergleich.savetoDB(
							conn, Clientvcfvergleich.snpidlist
									.get(Clientvcfvergleich.getpositionwild(rsp.getString("SNPID"))).toString(),
							"MISS");

				}

			}
			final String wildy = Clientvcfvergleich.getwildtype(rsp.getString("SNPID"));
			// ----------------check for null results
			if (rsp.getString("match_result") == null) {

				if (Clientvcfvergleich.getrsbtnstuffwithsnpid(rsp.getString("SNPID")) == "REV") {
					final String temppypy = Clientvcfvergleich.swap(pypy);
					if (Clientvcfvergleich.checkresults(temppypy, wildy, wildy)) {
						Clientvcfvergleich.savetoDB(conn, rsp.getString("SNPID"), "OK");
					} else {
						Clientvcfvergleich.savetoDB(conn, rsp.getString("SNPID"), "MISS");
					}
				} else {
					if (Clientvcfvergleich.getrsbtnstuffwithsnpid(rsp.getString("SNPID")) == "FOD") {
						if (Clientvcfvergleich.checkresults(pypy, wildy, wildy)) {
							Clientvcfvergleich.savetoDB(conn, rsp.getString("SNPID"), "OK");
						} else {
							Clientvcfvergleich.savetoDB(conn, rsp.getString("SNPID"), "MISS");
						}
					}
				}
			}
			// -----------------------------------------------------------writing happens
			// here->
			empinfo.put(Integer.toString(ki + 2),
					new Object[] { rsp.getString("journal_number"),
							Clientvcfvergleich.getrsname(rsp.getString("SNPID")), rsp.getString("match_result"), pypy,
							rsp.getString("quality"), Clientvcfvergleich.getchromi(rsp.getString("SNPID")),
							Clientvcfvergleich.getexo(rsp.getString("SNPID"), wildy, ki), wildy,
							Clientvcfvergleich.getrsbtnstuffwithsnpid(rsp.getString("SNPID")) });
			w.write(rsp.getString("journal_number") + "\t\t" + Clientvcfvergleich.getrsname(rsp.getString("SNPID"))
					+ "\t\t" + rsp.getString("match_result") + "\t\t" + pypy + "\t\t" + rsp.getString("quality")
					+ "\t\t" + Clientvcfvergleich.getchromi(rsp.getString("SNPID")) + "\t\t"
					+ Clientvcfvergleich.getexo(rsp.getString("SNPID"), wildy, ki) + "\t\t" + wildy + "\t\t"
					+ Clientvcfvergleich.getrsbtnstuffwithsnpid(rsp.getString("SNPID")));
			w.newLine();
			if ("OK".equals(rsp.getString("match_result"))) {
				Clientvcfvergleich.okcounter++;
			}
			if ("MISS".equals(rsp.getString("match_result"))) {
				Clientvcfvergleich.misscounter++;
			}
			ki++;
		} // end while(rsp.next())
		w.write("#MISS: " + Clientvcfvergleich.misscounter + "\tOK: " + Clientvcfvergleich.okcounter);
		w.close();
		System.out.println("saved @ " + savepatq + "\n" + Clientvcfvergleich.exoList.size());

		final Set<String> keyid = empinfo.keySet();
		int rowid = 0;
		// style 1 for passed pyroresults
		final XSSFCellStyle style1 = workbook.createCellStyle();
		style1.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
		style1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		// style 2 for check quality
		final XSSFCellStyle style2 = workbook.createCellStyle();
		style2.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
		style2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		// style 3 for failed quality
		final XSSFCellStyle style3 = workbook.createCellStyle();
		style3.setFillForegroundColor(IndexedColors.RED.getIndex());
		style3.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		for (final String key : keyid) {

			final XSSFRow row = spreadsheet.createRow(rowid++);
			final Object[] objectArr = empinfo.get(key);
			int cellid = 0;

			for (final Object obj : objectArr) {

				final Cell cell = row.createCell(cellid++);
				cell.setCellValue((String) obj);
				if ("Passed".equals(cell.getStringCellValue())) {

					cell.setCellStyle(style1);
				} else if ("Check".equals(cell.getStringCellValue())) {

					cell.setCellStyle(style2);
				} else if ("Failed".equals(cell.getStringCellValue())) {

					cell.setCellStyle(style3);
				}

			}
		}
		/**
		 * einfügen der letzten Reihe
		 */
		empinfo.put(Integer.toString(ki + 2), new Object[] { "MISS: ", Integer.toString(Clientvcfvergleich.misscounter),
				"OK: ", Integer.toString(Clientvcfvergleich.okcounter) });
		final XSSFRow row = spreadsheet.createRow(24);
		final Object[] objectArr = empinfo.get(Integer.toString(ki + 2));
		int cellid = 0;

		for (final Object obj : objectArr) {
			final Cell cell = row.createCell(cellid++);
			cell.setCellValue((String) obj);
		}

		// Write the workbook in file system
		final FileOutputStream out = new FileOutputStream(
				new File(savepatq.substring(0, savepatq.lastIndexOf('.')) + ".xlsx"));
		workbook.write(out);
		out.close();
		workbook.close();
		System.out.println("Writesheet.xlsx written successfully");
		// -----------------------------------------------------------------------------------------------------------------------------------------------
	}

	/**
	 *
	 * @param string
	 * @return
	 */
	private static String getchromi(final String string) {
		String umi = "";
		for (int i = 0; i < Clientvcfvergleich.snpidlist.size(); i++) {
			if (string.equals(Clientvcfvergleich.snpidlist.get(i).toString())) {
				umi = Clientvcfvergleich.chrlist.get(i).toString();
			}
		}
		return umi;
	}

	/**
	 *
	 * @param snipzy
	 * @return
	 */
	private static String getrsbtnstuffwithsnpid(final String snipzy) {

		String nana = "";
		for (int i = 0; i < Clientvcfvergleich.snpidlist.size(); i++) {
			if (snipzy.equals(Clientvcfvergleich.snpidlist.get(i).toString())) {
				nana = Clientvcfvergleich.chrlist.get(i).toString();
				break;
			}
		}
		if (Clientvcfvergleich.btncheck(nana)) {
			return "REV";
		} else {
			return "FOD";
		}

	}

	/**
	 *
	 * @param string
	 * @return
	 */
	private static int getpositionwild(final String string) {
		int i = 0;
		for (int j = 0; j < Clientvcfvergleich.notelist.size(); j++) {
			if (Clientvcfvergleich.notelist.get(j).toString().equals(string)) {
				i = j;
				break;
			}
		}
		return i;
	}
/**
 * 
 * @param test
 * @return
 */
	private static String swapsinglebase(final String test) {
		String res = "";
		if (!test.isEmpty()) {

			// first base of results
			if (test.substring(0, 1).equals("A")) {
				res = res + "T";
			}
			if (test.substring(0, 1).equals("T")) {
				res = res + "A";
			}
			if (test.substring(0, 1).equals("G")) {
				res = res + "C";
			}
			if (test.substring(0, 1).equals("C")) {
				res = res + "G";
			}
		}
		return res;
	}

	/**
	 *
	 * @param string
	 * @return
	 */
	private static String getrsname(final String string) {
		final String rsname = null;
		int i = 0;
		while (rsname == null) {
			if (i < Clientvcfvergleich.snpidlist.size()) {
				if (string.equals(Clientvcfvergleich.snpidlist.get(i).toString())) {
					return Clientvcfvergleich.rsList.get(i).toString();
					// rsList.get(i)
				}
			} else {
				return null;
			}
			i++;
		}
		return null;
	}
/**
 * 
 * @param snpid
 * @param wildy
 * @param k
 * @return
 */
	private static String getexo(final String snpid, final String wildy, final int k) {
		String unused = "";
		int j = 0;
		for (int i = 0; i < Clientvcfvergleich.snpidlist.size(); i++) {
			if (snpid.equals(Clientvcfvergleich.snpidlist.get(i).toString())) {
				j = i;
				break;
			}
		}
		for (int k1 = 0; k1 < Clientvcfvergleich.exoList.size(); k1++) {
			if (Clientvcfvergleich.chrlist.get(j).equals(Clientvcfvergleich.exoList.get(k1).toString().substring(0,
					Clientvcfvergleich.exoList.get(k1).toString().lastIndexOf("\t\t")))) {
				unused = Clientvcfvergleich.exoList.get(k1).toString()
						.substring(Clientvcfvergleich.exoList.get(k1).toString().lastIndexOf("\t\t") + 1);
				break;
			}
		}
		if (unused.isEmpty()) {
			return wildy + "/" + wildy;
		} else {
			return unused;
		}
	}

	/**
	 * prints info text
	 */
	public static void printinfo() {
		System.out.println(
				"this java script can make a connection to the inhouseDB and check the vcf file of a patient with the"
						+ " genotypisation of this patient in the DB.");
		System.out.println();
		System.out.println("arguments:");
		System.out.println("help or ?\t\t opens this help text");
		System.out.println("first argument is the path to the vcf file");
		System.out.println("2nd argument is the save folder");
		System.out.println("3rd argument is the optional Zugangsdaten.pm flepath");
		System.out.println("required argument:");
		System.out.println(
				"path to vcf file \t & \t save folder are needed to start this java script. Filename should be like this: 0001-18.vcf");
		System.out.println();
		System.out.println("version 1.5.1");
		System.out.println("@author Christoph Steisslinger");
		System.out.println("contact: christoph.steisslinger@writeme.com");
	}

	/**
	 * gets the userinfo for connection from ZugangsDaten.pm
	 *
	 * @throws IOException
	 */
	private static void getuserinfo() throws IOException {
		int counter = 0;
		if (Clientvcfvergleich.zugangspm == null) {
			try (BufferedReader br = new BufferedReader(
					new FileReader("C:\\Users\\hg-user\\Downloads\\ZugangsDaten.pm"))) {
				String line = br.readLine();
				while (counter != 4) {
					if (line.startsWith("our $DB_USER")) {
						Clientvcfvergleich.username = line.substring(line.indexOf("'") + 1, line.lastIndexOf("'"));
						counter++;
					}
					if (line.startsWith("our $DB_PASSWD")) {
						Clientvcfvergleich.pw = line.substring(line.indexOf("'") + 1, line.lastIndexOf("'"));
						counter++;
					}
					if (line.startsWith("our $DATABASE")) {
						line.substring(line.indexOf("'") + 1, line.lastIndexOf("'"));
						counter++;
					}
					if (line.startsWith("our $HOST")) {
						line.substring(line.indexOf("'") + 1, line.lastIndexOf("'"));
						counter++;
					}

					line = br.readLine();
				}
			} catch (final FileNotFoundException e) {
				System.out.println("cant find ZugangsDaten.pm");
			}
		} else {
			try (BufferedReader br = new BufferedReader(new FileReader(Clientvcfvergleich.zugangspm))) {
				String line = br.readLine();
				while (counter != 4) {
					if (line.startsWith("our $DB_USER")) {
						Clientvcfvergleich.username = line.substring(line.indexOf("'") + 1, line.lastIndexOf("'"));
						counter++;
					}
					if (line.startsWith("our $DB_PASSWD")) {
						Clientvcfvergleich.pw = line.substring(line.indexOf("'") + 1, line.lastIndexOf("'"));
						counter++;
					}
					if (line.startsWith("our $DATABASE")) {
						line.substring(line.indexOf("'") + 1, line.lastIndexOf("'"));
						counter++;
					}
					if (line.startsWith("our $HOST")) {
						line.substring(line.indexOf("'") + 1, line.lastIndexOf("'"));
						counter++;
					}

					line = br.readLine();
				}
			} catch (final FileNotFoundException e) {
				System.out.println("cant find ZugangsDaten.pm");
			}
		}

	}

	/**
	 * checks snp pos to find snp, then compares the results of pyro and vcf & adds
	 * to resultList for printing to txt file
	 *
	 * @param connconn
	 * @throws SQLException
	 * @throws IOException
	 */
	private static void dostuff(final Connection connconn) throws SQLException, IOException {

		for (int i = 0; i < Clientvcfvergleich.resultlist.size(); i++) {
			boolean btn = false;
			if (Clientvcfvergleich.notelist.get(i).toString().equals(Clientvcfvergleich.POS_ID)) {
				// check if snp @ chr ... is Btn
				if (Clientvcfvergleich.btncheck(Clientvcfvergleich.chrlist.get(i).toString())) {
					btn = true;
					Clientvcfvergleich.resultlist.set(i,
							Clientvcfvergleich.swap(Clientvcfvergleich.resultlist.get(i).toString()));
				}
				// AF check
				if ("1".equals(Clientvcfvergleich.af)) {

					if (Clientvcfvergleich.checkresults(Clientvcfvergleich.resultlist.get(i).toString(),
							Clientvcfvergleich.qual, Clientvcfvergleich.qual)) {
						Clientvcfvergleich.exoList.add(Clientvcfvergleich.chr + "\t\t" + Clientvcfvergleich.qual + "/"
								+ Clientvcfvergleich.qual);
						if (btn) {
							Clientvcfvergleich.resultlist.set(i,
									Clientvcfvergleich.swap(Clientvcfvergleich.resultlist.get(i).toString()));
						}
						Clientvcfvergleich.savetoDB(connconn, Clientvcfvergleich.snpidlist.get(i).toString(), "OK");

					} else {
						if (btn) {
							Clientvcfvergleich.resultlist.set(i,
									Clientvcfvergleich.swap(Clientvcfvergleich.resultlist.get(i).toString()));
						}
						Clientvcfvergleich.exoList.add(Clientvcfvergleich.chr + "\t\t" + Clientvcfvergleich.qual + "/"
								+ Clientvcfvergleich.qual);
						Clientvcfvergleich.savetoDB(connconn, Clientvcfvergleich.snpidlist.get(i).toString(), "MISS");
						// not equal with pyro

					}

				} else {
					if (Clientvcfvergleich.checkresults(Clientvcfvergleich.resultlist.get(i).toString(),
							Clientvcfvergleich.alt, Clientvcfvergleich.qual)) {
						if (btn) {
							Clientvcfvergleich.resultlist.set(i,
									Clientvcfvergleich.swap(Clientvcfvergleich.resultlist.get(i).toString()));
						}
						Clientvcfvergleich.exoList.add(Clientvcfvergleich.chr + "\t\t" + Clientvcfvergleich.alt + "/"
								+ Clientvcfvergleich.qual);
						Clientvcfvergleich.savetoDB(connconn, Clientvcfvergleich.snpidlist.get(i).toString(), "OK");

					} else {
						if (btn) {
							Clientvcfvergleich.resultlist.set(i,
									Clientvcfvergleich.swap(Clientvcfvergleich.resultlist.get(i).toString()));
						}
						Clientvcfvergleich.exoList.add(Clientvcfvergleich.chr + "\t\t" + Clientvcfvergleich.alt + "/"
								+ Clientvcfvergleich.qual);
						Clientvcfvergleich.savetoDB(connconn, Clientvcfvergleich.snpidlist.get(i).toString(), "MISS");

						// not equal with pyro

					}
				}
			} // end if snppos.equals(POS_ID)

		} // end for

	}

	/**
	 *
	 * @param conn
	 * @param snpid
	 * @param qwertz
	 * @throws SQLException
	 */

	private static void savetoDB(final Connection conn, final String snpid, final String qwertz) throws SQLException {
		try {
			final String queryid = "update sample_pyro sp set match_result='" + qwertz + "' where sp.SNPID='" + snpid
					+ "' and sp.journal_number= '" + Clientvcfvergleich.journal_number + "' ;";
			final Statement sti = conn.createStatement();
			sti.executeUpdate(queryid);
			sti.close();
		} catch (final SQLException s) {
			System.out.println("updating sample_pyro table in DB IHDB failed. More info:" + s.getMessage());
		}
	}

	/**
	 * are results equal?
	 *
	 * @param resultpyro
	 * @param alt2
	 * @param qual2
	 * @return boolean
	 */
	private static boolean checkresults(final String resultpyro, final String alt2, final String qual2) {
		final String matchA = alt2 + "/" + qual2;
		final String matchB = qual2 + "/" + alt2;
		if (resultpyro.equals(matchA)) {

			return true;
		} else {
			if (resultpyro.equals(matchB)) {

				return true;
			}
		}
		return false;
	}

	/**
	 * splits the vcf line into chr, pos, alt, qual
	 *
	 * @param String
	 *            sth
	 */
	private static void splittopos(final String sth) {
		if (!sth.isEmpty()) {
			String opferline;
			Clientvcfvergleich.alline = sth;

			Clientvcfvergleich.chr = Clientvcfvergleich.alline.substring(0, Clientvcfvergleich.alline.indexOf("\t"));
			Clientvcfvergleich.chr = Clientvcfvergleich.chr.substring(3);
			opferline = Clientvcfvergleich.alline.substring(Clientvcfvergleich.alline.indexOf("\t") + 1,
					Clientvcfvergleich.alline.length());
			Clientvcfvergleich.POS_ID = opferline.substring(0, opferline.indexOf("\t"));
			opferline = opferline.substring(opferline.indexOf("\t") + 1, opferline.length());
			opferline.substring(0, opferline.indexOf("\t"));// .
			opferline = opferline.substring(opferline.indexOf("\t") + 1, opferline.length());
			Clientvcfvergleich.alt = opferline.substring(0, opferline.indexOf("\t"));// alt
			opferline.substring(0, opferline.indexOf("\t"));
			opferline = opferline.substring(opferline.indexOf("\t") + 1, opferline.length());
			Clientvcfvergleich.qual = opferline.substring(0, opferline.indexOf("\t"));
			opferline.substring(0, opferline.indexOf("\t"));// qual
			opferline = opferline.substring(opferline.indexOf("\t") + 1, opferline.length());
			opferline.substring(0, opferline.length());// filter+info
			Clientvcfvergleich.af = opferline.substring(opferline.lastIndexOf(";AF=") + 4,
					opferline.lastIndexOf(";AF=") + 5);

		} else {
			System.out.println("empty lines in vcf-file");
		}
	}

	/**
	 * swaps base when snp is btn
	 *
	 * @param test
	 * @return
	 */
	private static String swap(final String test) {
		String res = "";
		if (!test.isEmpty()) {

			// first base of results
			if (test.substring(0, 1).equals("A")) {
				res = res + "T";
			}
			if (test.substring(0, 1).equals("T")) {
				res = res + "A";
			}
			if (test.substring(0, 1).equals("G")) {
				res = res + "C";
			}
			if (test.substring(0, 1).equals("C")) {
				res = res + "G";
			}
			res = res + "/";
			// secound base
			if (test.substring(2).equals("A")) {
				res = res + "T";
			}
			if (test.substring(2).equals("T")) {
				res = res + "A";
			}
			if (test.substring(2).equals("G")) {
				res = res + "C";
			}
			if (test.substring(2).equals("C")) {
				res = res + "G";
			}
		}
		return res;
	}

	/**
	 * checks if snp is btn
	 *
	 * @param chr
	 * @return
	 */
	private static boolean btncheck(final String chr) {
		switch (chr) {
		case "1":
			return true;// btn
		case "2":
			return false;
		case "3":
			return true;// btn
		case "4":
			return false;
		case "5":
			return false;
		case "6":
			return false;
		case "7":
			return false;
		case "8":
			return false;
		case "9":
			return true;// btn
		case "10":
			return true;// btn
		case "11":
			return false;
		case "12":
			return false;
		case "13":
			return false;
		case "14":
			return false;
		case "15":
			return true;// btn
		case "16":
			return false;
		case "17":
			return true;// btn
		case "18":
			return false;
		case "19":
			return true;// btn
		case "20":
			return true;// btn
		case "21":
			return true;// btn
		case "22":
			return true;// btn
		default:
			return false;// should never happen
		}
	}

	private static String getwildtype(final String snpid) {
		// find snpid in arraylist to get position of rsname
		int pos = 0;
		for (int i = 0; i < Clientvcfvergleich.snpidlist.size(); i++) {
			if (Clientvcfvergleich.snpidlist.get(i).toString().equals(snpid)) {
				pos = i;
			}
		}
		switch (Clientvcfvergleich.rsList.get(pos)) {
		case "rs1410592":
			return "G";
		case "rs497692":
			return "T";
		case "rs2819561":
			return "A";
		case "rs4688963":
			return "T";
		case "rs309557":
			return "T";
		case "rs2942":
			return "G";
		case "rs17548783":
			return "T";
		case "rs4735258":
			return "T";
		case "rs1381532":
			return "A";
		case "rs10883099":
			return "G";
		case "rs4617548":
			return "A";
		case "rs7300444":
			return "C";
		case "rs9532292":
			return "A";
		case "rs2297995":
			return "G";
		case "rs4577050":
			return "G";
		case "rs2070203":
			return "G";
		case "rs1037256":
			return "G";
		case "rs9962023":
			return "T";
		case "rs2228611":
			return "T";
		case "rs10373":
			return "A";
		case "rs4148973":
			return "T";
		case "rs4675":
			return "T";
		default:
			return "new SNP not added in List";
		}

	}

	// only getter and setter down there.
	/**
	 *
	 * @return
	 */
	public static String getAlline() {
		return Clientvcfvergleich.alline;
	}

	/**
	 *
	 * @param alline
	 */
	public static void setAlline(final String alline) {
		Clientvcfvergleich.alline = alline;
	}

	/**
	 *
	 * @return
	 */
	public static String getPOS_ID() {
		return Clientvcfvergleich.POS_ID;
	}

	/**
	 *
	 * @param pOS_ID
	 */
	public static void setPOS_ID(final String pOS_ID) {
		Clientvcfvergleich.POS_ID = pOS_ID;
	}

	/**
	 *
	 * @return
	 */
	public static String getAlt() {
		return Clientvcfvergleich.alt;
	}

	/**
	 *
	 * @param alt
	 */
	public static void setAlt(final String alt) {
		Clientvcfvergleich.alt = alt;
	}

	/**
	 *
	 * @return
	 */
	public static String getQual() {
		return Clientvcfvergleich.qual;
	}

	/**
	 *
	 * @param qual
	 */
	public static void setQual(final String qual) {
		Clientvcfvergleich.qual = qual;
	}

	/**
	 *
	 * @return
	 */
	public static String getMyUrl() {
		return Clientvcfvergleich.myUrl;
	}

	/**
	 *
	 * @param myUrl
	 */
	public static void setMyUrl(final String myUrl) {
		Clientvcfvergleich.myUrl = myUrl;
	}

	/**
	 *
	 * @return
	 */
	public static String getJournal_number() {
		return Clientvcfvergleich.journal_number;
	}

	/**
	 *
	 * @param journal_number
	 */
	public static void setJournal_number(final String journal_number) {
		Clientvcfvergleich.journal_number = journal_number;
	}

	/**
	 * @return the snpidlist
	 */
	public static ArrayList<String> getSnpidlist() {
		return Clientvcfvergleich.snpidlist;
	}

	/**
	 * @param snpidlist
	 *            the snpidlist to set
	 */
	public static void setSnpidlist(final ArrayList<String> snpidlist) {
		Clientvcfvergleich.snpidlist = snpidlist;
	}

	/**
	 * @return the chrlist
	 */
	public static ArrayList<String> getChrlist() {
		return Clientvcfvergleich.chrlist;
	}

	/**
	 * @param chrlist
	 *            the chrlist to set
	 */
	public static void setChrlist(final ArrayList<String> chrlist) {
		Clientvcfvergleich.chrlist = chrlist;
	}

	/**
	 * @return the resultlist
	 */
	public static ArrayList<String> getResultlist() {
		return Clientvcfvergleich.resultlist;
	}

	/**
	 * @param resultlist
	 *            the resultlist to set
	 */
	public static void setResultlist(final ArrayList<String> resultlist) {
		Clientvcfvergleich.resultlist = resultlist;
	}

	/**
	 * @return the notelist
	 */
	public static ArrayList<String> getNotelist() {
		return Clientvcfvergleich.notelist;
	}

	/**
	 * @param notelist
	 *            the notelist to set
	 */
	public static void setNotelist(final ArrayList<String> notelist) {
		Clientvcfvergleich.notelist = notelist;
	}

	/**
	 *
	 * @return
	 */
	public static ArrayList<String> getRsList() {
		return Clientvcfvergleich.rsList;
	}

	/**
	 *
	 * @param rsList
	 */
	public static void setRsList(final ArrayList<String> rsList) {
		Clientvcfvergleich.rsList = rsList;
	}

	/**
	 *
	 * @return
	 */
	public static String getChr() {
		return Clientvcfvergleich.chr;
	}

	/**
	 *
	 * @param chr
	 */
	public static void setChr(final String chr) {
		Clientvcfvergleich.chr = chr;
	}

	/**
	 *
	 * @return
	 */
	public static String getMyUrl1() {
		return Clientvcfvergleich.myUrl1;
	}

	/**
	 *
	 * @param myUrl1
	 */
	public static void setMyUrl1(final String myUrl1) {
		Clientvcfvergleich.myUrl1 = myUrl1;
	}

	/**
	 *
	 * @return
	 */
	public static String getMyUrl2() {
		return Clientvcfvergleich.myUrl2;
	}

	/**
	 *
	 * @param myUrl2
	 */
	public static void setMyUrl2(final String myUrl2) {
		Clientvcfvergleich.myUrl2 = myUrl2;
	}

	/**
	 *
	 * @return
	 */
	public static ArrayList<String> getPrintList() {
		return Clientvcfvergleich.printList;
	}

	/**
	 *
	 * @param printList
	 */
	public static void setPrintList(final ArrayList<String> printList) {
		Clientvcfvergleich.printList = printList;
	}

	/**
	 *
	 * @return
	 */
	public static ArrayList<String> getSaveList() {
		return Clientvcfvergleich.saveList;
	}

	/**
	 *
	 * @param saveList
	 */
	public static void setSaveList(final ArrayList<String> saveList) {
		Clientvcfvergleich.saveList = saveList;
	}

	/**
	 *
	 * @return
	 */
	public static ArrayList<String> getExoList() {
		return Clientvcfvergleich.exoList;
	}

	/**
	 *
	 * @param exoList
	 */
	public static void setExoList(final ArrayList<String> exoList) {
		Clientvcfvergleich.exoList = exoList;
	}

	/**
	 *
	 * @return
	 */
	public static String getUsername() {
		return Clientvcfvergleich.username;
	}

	/**
	 *
	 * @param username
	 */
	public static void setUsername(final String username) {
		Clientvcfvergleich.username = username;
	}

	/**
	 *
	 * @return
	 */
	public static String getPw() {
		return Clientvcfvergleich.pw;
	}

	/**
	 *
	 * @param pw
	 */
	public static void setPw(final String pw) {
		Clientvcfvergleich.pw = pw;
	}

	/**
	 *
	 * @return
	 */
	public static String getZugangspm() {
		return Clientvcfvergleich.zugangspm;
	}

	/**
	 *
	 * @param zugangspm
	 */
	public static void setZugangspm(final String zugangspm) {
		Clientvcfvergleich.zugangspm = zugangspm;
	}

	/**
	 *
	 * @return
	 */
	public static int getOkcounter() {
		return Clientvcfvergleich.okcounter;
	}

	/**
	 *
	 * @param okcounter
	 */
	public static void setOkcounter(final int okcounter) {
		Clientvcfvergleich.okcounter = okcounter;
	}

	/**
	 *
	 * @return
	 */
	public static int getMisscounter() {
		return Clientvcfvergleich.misscounter;
	}

	/**
	 *
	 * @param misscounter
	 */
	public static void setMisscounter(final int misscounter) {
		Clientvcfvergleich.misscounter = misscounter;
	}

	/**
	 *
	 * @return
	 */
	public static String getAf() {
		return Clientvcfvergleich.af;
	}

	/**
	 *
	 * @param af
	 */
	public static void setAf(final String af) {
		Clientvcfvergleich.af = af;
	}

	/**
	 * to String
	 * 
	 * @return Class and hashCode
	 */
	@Override
	public String toString() {
		return "Clientvcfvergleich [getClass()=" + this.getClass() + ", hashCode()=" + this.hashCode() + ", toString()="
				+ super.toString() + "]";
	}

}
