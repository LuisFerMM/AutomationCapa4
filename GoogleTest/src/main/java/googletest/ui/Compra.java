package googletest.ui;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.xmlbeans.GDate;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Sleeper;

public class Compra {

	private static int valorCompra;
	private static GestorDeArchivos gda;

	public static void main(String[] args) throws ParseException, InterruptedException {

		// Cargar la información de excel con una ventana

		WebDriver driver = null;

		gda = new GestorDeArchivos();

		File cd = gda.getFileChromeDriver();
		File excel = gda.getFileExcel();

		if (cd != null && excel != null) {
			String rutaCD = cd.getAbsolutePath();
			String rutaExcel = excel.getAbsolutePath();
			ArrayList<Chip> chips = gda.convertirXlsxADatos();
			System.setProperty("webdriver.chrome.driver", rutaCD);
			driver = new ChromeDriver();
			driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
			driver.get("https://miportal.entel.cl/");
			// posible sleep
			boolean primeraVez = true;
			int intentos = 0;
			// Iteraciones mientras lee el excel
			for (int i = 0; i < chips.size(); i++) {

				try {
					Date date = new Date();
					String url = driver.getCurrentUrl();
					int chance = 6;

//					caso de que no cargue el menu principal
//						driver.close();
//						driver = new ChromeDriver();
//						driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
//						driver.get("https://miportal.entel.cl/");
//						System.out.println( "Intento de retomar compras tras caida de la pagina");
//						i--;
//						intentos++;
//						continue;
//					

					if (!iniciarSesion(driver, url, chips.get(i))) {
						modificarDatosDe(chips.get(i), chips.get(i).getSaldo(), chips.get(i).getSaldo(), date,
								"Problema inicio de sesión");
						continue;
					}
					url = driver.getCurrentUrl();

					// Obtiene el saldo anterior a la compra y valida que no supere el saldo
					String saldoString = driver
							.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
							.getText();
					int saldoInt = obtenerValorEnInt(saldoString);
					if(chips.get(i).fueComprado())
						if(saldoInt != chips.get(i).getSaldo()) {
							modificarDatosDe(chips.get(i), chips.get(i).getSaldo(), saldoInt, date, "Ok");
							continue;
						}
					if (primeraVez) {
						seleccionarBolsa(driver);
						String total = driver.findElement(By.xpath("//*[@id=\"Voz\"]/div/div[2]/div[2]")).getText();
						valorCompra = obtenerValorEnInt(total);
						primeraVez = false;
					}
					if (saldoInt < valorCompra) {
						cerrarSesion(driver);
						modificarDatosDe(chips.get(i), saldoInt, saldoInt, date, "Saldo insuficiente");
					} else {
						if (driver.getCurrentUrl().equals(url))
							seleccionarBolsa(driver);
						int nuevoSaldo = terminarProcesoDeCompra(driver, saldoString, url, 1, i);
						if (nuevoSaldo == -1 && intentos < 3) {
							i--;
							intentos++;
							continue;
						}
						cerrarSesion(driver);
						modificarDatosDe(chips.get(i), saldoInt, nuevoSaldo, date, "Ok");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(new JFrame(), "Error al cargar componentes");
				} catch (WebDriverException e2) {
					if (intentos < 6) {
						e2.printStackTrace();
						System.out.println("Caída inesperada de internet/servidor");
						if (!chips.get(i).getPostCompra().equals("Ok")) {
							chips.get(i).pierdeOportunidad();
							if (chips.get(i).getOportunidades() == 0) {
								modificarDatosDe(chips.get(i), chips.get(i).getSaldo(), chips.get(i).getSaldo(), new Date(), "Problema relacionado con la página");
								i++;
							}
							i--;
						}
						driver.quit();
						driver = new ChromeDriver();
						driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
						driver.get("https://miportal.entel.cl/");
						System.out.println("Intento de retomar compras tras caida de la pagina");
						intentos++;
						continue;
					} else
						break;
				} catch (Exception e3) {
					e3.printStackTrace();
					Logger registro = Logger.getLogger("MyLog");
					FileHandler fh;

					try {
						fh = new FileHandler("C:\\Users\\Luis\\OneDrive - Universidad Icesi\\Capa4\\verRegistro", true);
						registro.addHandler(fh);

						SimpleFormatter formatter = new SimpleFormatter();
						fh.setFormatter(formatter);

						registro.info(e3.getMessage());

					} catch (SecurityException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			gda.generarExcel();
		}
		driver.quit();
		System.exit(0);
	}

	private static void modificarDatosDe(Chip chip, int saldoInt, int nuevoSaldo, Date fechaActual, String postCompra) {
		String seCompro = "";
		if (saldoInt == nuevoSaldo) {
			seCompro = "Problema al comprar";
			if (postCompra.equals("Ok")) {
				postCompra = "Saldo sin descontar";
			}
		}
		chip.setSaldo(saldoInt, nuevoSaldo);
		chip.setPostCompra(postCompra);
		DateFormat horaInicio = new SimpleDateFormat("HH:mm");
		DateFormat fechaInicio = new SimpleDateFormat("dd-MMM-yyyy");
		chip.setFechaInicio(fechaActual);
		chip.setHoraCompra(fechaActual);
		System.out.println(chip.getNumeroLista() + " " + chip.getNumeroSim() + " " + chip.getRut() + " "
				+ chip.getClave() + " " + nuevoSaldo + " " + horaInicio.format(fechaActual) + " "
				+ fechaInicio.format(fechaActual) + " " + seCompro);
	}

	private static void seleccionarBolsa(WebDriver driver) throws InterruptedException {
		driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/a[2]/span")).click();
		driver.findElement(By.xpath("//*[@id=\"tab_prepago\"]/div/a")).click();
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[1]/div[2]/div[1]/form/div/div[3]/div/input")).click();
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[1]/div[2]/div[1]/form/div/div[1]/div/input")).click();
		Thread.sleep(600);
	}

	private static int obtenerValorEnInt(String saldoString) throws ParseException {
		int saldoInt;
		NumberFormat formatter = NumberFormat.getInstance();
		formatter.setParseIntegerOnly(true);
		if (saldoString.startsWith("$"))
			saldoInt = formatter.parse(saldoString.substring(1)).intValue();
		else
			saldoInt = Integer.parseInt(saldoString);
		return saldoInt;
	}

	private static void cerrarSesion(WebDriver driver) throws InterruptedException {
		if (driver.getCurrentUrl().equals("https://miportal.entel.cl/miEntel/mi-cuenta")) {
			driver.findElement(By.xpath("//*[@id=\"myAccountDropDownLink\"]/a/div")).click();
			Thread.sleep(400);
			driver.findElement(By.xpath("//*[@id=\"action-PROFILE_LOGOUT-1\"]/span")).click();
		} else {
			driver.findElement(By.xpath("//*[@id=\"header\"]/div/ul/li/a")).click();
			Thread.sleep(400);
			driver.findElement(By.id("action-PROFILE_LOGOUT")).click();
		}
		Thread.sleep(3000);
	}

	private static int terminarProcesoDeCompra(WebDriver driver, String saldoString, String url, int intento, int i)
			throws InterruptedException, ParseException {
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[2]/div[2]/div[2]/span[2]/input")).click();
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[2]/div[2]/div[2]/span[2]/span[3]")).click();

		int validaciones = 0;
		while (!driver.getCurrentUrl().equals("https://miportal.entel.cl/bolsas/confirmacion-compra-bolsas")
				&& validaciones < 6) {
			Thread.sleep(600);
			validaciones++;
		}
		if (!driver.getCurrentUrl().equals("https://miportal.entel.cl/bolsas/confirmacion-compra-bolsas") && intento < 2) {
			intento++;
			driver.get("https://miportal.entel.cl/miEntel/mi-cuenta");
			seleccionarBolsa(driver);
			return terminarProcesoDeCompra(driver, saldoString, url, intento, i);
		} else if (driver.getCurrentUrl().equals("https://miportal.entel.cl/bolsas/confirmacion-compra-bolsas")){
			gda.getChips().get(i).setEstadoCompra(true);
		}
		// Revisa si la compra se realizó exitosamente
		Thread.sleep(9000);
		int times = 4;
		do {
			driver.get("https://miportal.entel.cl/miEntel/mi-cuenta");
//			if(!verificarCaidaServerOInternet("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2",
//					"https://miportal.entel.cl/miEntel/mi-cuenta", driver))
//				return -1;
			if (!driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2")).getText()
					.equals(saldoString))
				break;
			Thread.sleep(3000);
			times--;
		} while (times > 0);
		String saldoReciente = driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
				.getText();
		return obtenerValorEnInt(saldoReciente);
	}

//	private static boolean verificarCaidaServerOInternet(String xpath, String page, WebDriver driver)
//			throws InterruptedException {
//		boolean correcto = true;
//		if (!existsElementByXPath(driver, xpath))
//			driver.get(page);
//		int chance = 6;
//		while (!existsElementByXPath(driver, xpath) && chance > 0) {
//			Thread.sleep(600);
//			chance--;
//		}
//		if (!existsElementByXPath(driver, xpath)) {
//			correcto = false;
//			driver.close();
//			driver = new ChromeDriver();
//			driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
//			driver.get("https://miportal.entel.cl/");
//			System.out.println("Intento de retomar compras tras caida de la pagina");
//		}
//		return correcto;
//	}

//	/html/body/h1 <= bad request
//	/html/body/p
	private static boolean iniciarSesion(WebDriver driver, String url, Chip entrada) throws InterruptedException {
		driver.findElement(By.className("text")).click();
		String subWin = null;
		ingresarDatosDeSesion(driver, entrada, url);

		if (driver.getCurrentUrl().equals(url)) {
			driver.findElement(By.name("username")).clear();
			driver.findElement(By.name("rutt")).clear();
			driver.findElement(By.name("password")).clear();
			ingresarDatosDeSesion(driver, entrada, url);
		}
//		if(existsElementByClass(driver, "modal-content"))
//			driver.findElement(By.className("icon-close-overly")).click();
		driver.navigate().refresh();
		if (driver.getCurrentUrl().equals(url))
			return false;
		return true;
	}

	private static void ingresarDatosDeSesion(WebDriver driver, Chip entrada, String url) throws InterruptedException {
		Thread.sleep(600);
		driver.findElement(By.name("username")).sendKeys("" + entrada.getNumeroSim());
		driver.findElement(By.name("rutt")).sendKeys("" + entrada.getRut());
		driver.findElement(By.name("password")).sendKeys("" + entrada.getClave());
		Thread.sleep(400);
		driver.findElement(By.id("action-PROFILE_LOGIN")).click();
		int cont = 6;
		while (driver.getCurrentUrl().equals(url) && cont > 0) {
			Thread.sleep(2000);
			cont--;
		}
	}

}
