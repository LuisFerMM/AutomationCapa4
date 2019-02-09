package googletest.ui;

import java.awt.AWTException;
import java.awt.Robot;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.Validator;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import com.sun.glass.events.KeyEvent;

public class Main {

	private static int valorCompra;

	public static void main(String[] args) throws InterruptedException, ParseException {

//		Cargar la información de excel con una ventana

		WebDriver driver;
		System.setProperty("webdriver.chrome.driver",
				"C:\\Users\\Luis\\OneDrive - Universidad Icesi\\Instaladores\\chromedriver.exe");
		driver = new ChromeDriver();

		cargarPaginaEntel(driver);
		String url = driver.getCurrentUrl();

		iniciarSesion(driver, url);
		url = driver.getCurrentUrl();

//		Obtiene el saldo anterior a la compra y valida que no supere el saldo
		String saldoString = driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2"))
				.getText();
		int saldoInt = obtenerValorEnInt(saldoString);
		if (valorCompra == 0) {
			seleccionarBolsa(driver, url);
			String total = driver.findElement(By.xpath("//*[@id=\"Voz\"]/div/div[2]/div[2]")).getText();
			valorCompra = obtenerValorEnInt(total);
		}
		if (saldoInt < valorCompra) {
			cerrarSesion(driver);
		} else {
			terminarProcesoDeCompra(driver, saldoString, url);
		}
//		driver.close();
	}

	private static void seleccionarBolsa(WebDriver driver, String url) throws InterruptedException {
		driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/a[2]/span")).click();
		while (driver.getCurrentUrl().equals(url))
			Thread.sleep(500);
		driver.findElement(By.xpath("//*[@id=\"tab_prepago\"]/div/a")).click();
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[1]/div[2]/div[1]/form/div/div[3]/div/input")).click();
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[1]/div[2]/div[1]/form/div/div[1]/div/input")).click();
		Thread.sleep(200);
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
		driver.findElement(By.xpath("//*[@id=\"header\"]/div/ul/li/a")).click();
		Thread.sleep(200);
		driver.findElement(By.id("action-PROFILE_LOGOUT")).click();
	}

	private static void terminarProcesoDeCompra(WebDriver driver, String saldoString, String url)
			throws InterruptedException {
		seleccionarBolsa(driver, url);
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[2]/div[2]/div[2]/span[2]/input")).click();
		driver.findElement(By.xpath("//*[@id=\"prepago\"]/div/div[2]/div[2]/div[2]/span[2]/span[3]")).click();

//		Revisa si la compra se realizó exitosamente
		driver.findElement(By.xpath("//*[@id=\"header\"]/div/ul/li/a")).click();
		Thread.sleep(8000);
		int times = 3;
		while (driver.findElement(By.xpath("//*[@id=\"mobileAssetBalance\"]/div/div/div[1]/div/div/h2")).getText()
				.equals(saldoString) || times != 0) {
			driver.get("https://miportal.entel.cl/miEntel/mi-cuenta");
			Thread.sleep(2000);
			times--;
		}
	}

	private static void iniciarSesion(WebDriver driver, String url) throws InterruptedException {
		driver.findElement(By.name("username")).sendKeys("978812784");
		driver.findElement(By.name("rutt")).sendKeys("164858189");
		driver.findElement(By.name("password")).sendKeys("1536");
		driver.findElement(By.id("action-PROFILE_LOGIN")).click();
		while (driver.getCurrentUrl().equals(url))
			Thread.sleep(1000);
		driver.navigate().refresh();
		Thread.sleep(2000);
	}

	private static void cargarPaginaEntel(WebDriver driver) throws InterruptedException {
		driver.get("https://miportal.entel.cl/");
		driver.findElement(By.className("text")).click();
		String subWin = null;
		Thread.sleep(600);
	}

}
