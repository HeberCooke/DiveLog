/*
 * Heber Cooke  5/9/2019
 * 
 * Dive log 
 * software development capstone project
 * 
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class DiveLog extends Application {

	public static void main(String[] args) {

		Application.launch(args);
	}

	private String name = "";
	private String password = "";

	private DataOutputStream toServer;
	private DataInputStream fromServer;
	private Socket socket;
	private Integer diveNo = 1;
	int planDepth;
	int planHrs;
	int planMin;
	int rNt; // residual nitrogen time
	char pG1; // pressure group 1
	char pG2;// pressure group 2
	int savePlanDepth;

	// current user
	User user = new User();

	@Override
	public void start(Stage primaryStage) throws Exception {

		GridPane pane = new GridPane();
		// attempt to color the background  
		//pane.setStyle("-fx-background-color: linear-gradient(#e6ffff, #2eb8b8);");//#2eb8b8 //#00b3b3
		DropShadow ds = new DropShadow();
		ds.setOffsetY(3.0f);
		ds.setColor(Color.color(0.4f, 0.4f, 0.4f));

		//welcome text for login screen
		Text tf = new Text();
		tf.setEffect(ds);
		tf.setCache(true);
		tf.setX(10.0f);
		tf.setY(270.0f);
		tf.setFill(Color.BLUE);
		tf.setText("Welcome");
		tf.setFont(Font.font(null, FontWeight.BOLD, 32));

		TextField tfName = new TextField();
		tfName.setStyle("-fx-text-fill: steelblue;-fx-font-size: 14px;-fx-font-weight: bold");
		tfName.setPromptText("Name");

		PasswordField tfPass = new PasswordField();
		tfPass.setStyle("-fx-text-fill: steelblue;-fx-font-size: 14px;-fx-font-weight: bold");
		tfPass.setPromptText("Password");

		Button btn = new Button("Login");
		btn.setStyle("-fx-text-fill: steelblue;-fx-font-size: 14px;-fx-font-weight: bold"); // style 4

		Button btnSignUp = new Button("Sign up");
		btnSignUp.setStyle("-fx-text-fill: steelblue;-fx-font-size: 14px;-fx-font-weight: bold"); // style 4

		// label to set login error info ,sign in , log in, use different pass
		Label text = new Label();
		text.setStyle("-fx-text-fill: red; -fx-font-size: 13; -fx-font-weight: normal;"); 
		pane.add(tf, 1, 0);
		pane.add(tfName, 1, 1);
		pane.add(tfPass, 1, 2);
		pane.add(btn, 1, 3);
		pane.add(btnSignUp, 1, 4);
		pane.add(text, 1, 5);
		pane.setVgap(5);
		pane.setAlignment(Pos.CENTER);
		primaryStage.setResizable(false);
		Scene scene = new Scene(pane, 250, 250);
		primaryStage.setTitle("Login");
		primaryStage.setScene(scene);
		primaryStage.show();

		// Sign up Button sends the name and password to the server the server
		//checks them to the users list 
		btnSignUp.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {

				try {
					// clearing text
					text.setText("");

					name = tfName.getText().trim();
					password = tfPass.getText().trim();
					toServer.writeBoolean(true);
					toServer.writeUTF(name);
					toServer.writeUTF(password);
					toServer.flush();

					//clears the text fields
					tfName.clear();
					tfPass.clear();

					String s = fromServer.readUTF();
					if (s.equals("UseDiffPass")) {
						text.setText("use a diferent password");
					}
					if (s.equals("LogIn")) {
						text.setText("Log in");
					}
					// clearing s for next method
					s = "";

				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}

		});

		// log in button checks the user name and password for the name and password in the list
		btn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {

				try {
					// clearing text
					text.setText("");

					name = tfName.getText().trim();
					password = tfPass.getText().trim();
					toServer.writeBoolean(false);

					// checks to make sure there is a name
					if (name != "") {
						toServer.writeUTF(name);
						if (password != "") {
							toServer.writeUTF(password);
						}
					}
					toServer.flush();

					// clearing name and password text fields 
					tfName.clear();
					tfPass.clear();

					String s = fromServer.readUTF();
					// checking if password is in list
					if (s.equals("LoginSucsess")) {

						// creating a current user
						user = new User(name, password);

						// setting user to user from file
						user = logsFromFile();

						// starting the main Log stage or main screen 
						mainLog(primaryStage);

					} else if (s.equals("SignUp")) {
						text.setText("SignUp");
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});

		try {

			 socket = new Socket("localhost", 8000);
			fromServer = new DataInputStream(socket.getInputStream());
			toServer = new DataOutputStream(socket.getOutputStream());
			
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}// end start

	// Method gets user from file and returns user
	public User logsFromFile() {

		String fileNameEnter = name + "Data" + ".dat";
		File f = new File(fileNameEnter);

		if (f.exists()) { // if file exists read from file if not user is a new user
			try {
				ObjectInputStream inFromFile = new ObjectInputStream(new FileInputStream(f));

				user = (User) inFromFile.readObject();

				inFromFile.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}

		}
		return user;
	}

	int tempDiveNumber; // Global integer used in button next and button previous to navigate logs

	// main log is for main screen with fill out items 
 	public void mainLog(Stage primaryStage) {
		// styles--------------------------------
		String style = "-fx-text-fill: blue;-fx-font-size: 16px;-fx-font-weight: bold"; // main blue bold text
		String style2 = "-fx-text-fill: steelblue;-fx-font-size: 16px;-fx-font-weight: bold";
		// most changing text and input text
		String style3 = "-fx-text-fill: steelblue;-fx-font-size: 12px;-fx-font-weight: bold";
		String style4 = "-fx-text-fill: steelblue;-fx-font-size: 14px;-fx-font-weight: bold";
		String style5 = "-fx-text-fill: blue;-fx-font-size: 10px;-fx-font-weight: bold"; // Mini style for depth profile	
		Insets inSet = new Insets(10, 10, 10, 10); // default padding
		
		// main pane
		BorderPane pane = new BorderPane();
	//	pane.setStyle("-fx-background-color: linear-gradient(#e6ffff, #2eb8b8);");
		
		// shadow for diver picture
		DropShadow ds = new DropShadow();
		ds.setOffsetY(7.0f);
		ds.setColor(Color.color(0.4f, 0.4f, 0.4f));

		// Button  planner to go to planner screen 
		Button btnPlanner = new Button("Planner");
		btnPlanner.setPrefSize(80, 30);
		btnPlanner.setStyle(style4);
		btnPlanner.setEffect(ds);
		
		// Image scuba
		Image image = new Image("pics/diver_PNG10.png", 100, 48, false, false);
		ImageView imageView = new ImageView(image);

		// Create a Label with label and Icon
		Label lblScuba = new Label("", imageView);
		lblScuba.setPrefSize(60, 40);
		lblScuba.setEffect(ds);

		//Button log to go to  log screen
		Button btnLog = new Button("Log");
		btnLog.setPrefSize(80, 30);
		btnLog.setStyle(style4);
		btnLog.setEffect(ds);

		// hBox for top buttons
		HBox top = new HBox();
		top.setPadding(inSet);
		top.setSpacing(20);
		top.setAlignment(Pos.CENTER);
		top.getChildren().addAll(btnPlanner, lblScuba, btnLog);
		pane.setTop(top);

		//opening log screen and filling out user logs
		btnLog.setOnAction(e -> {

			// setting dive number to user log size to work with buttons previous and next
			// buttons
			tempDiveNumber = user.logs.size() - 1;

			Stage stage = new Stage();
			BorderPane bP = new BorderPane();
			bP.setPadding(inSet);

			// dive log pic for the log screen
			Image imgLog = new Image("pics/diveLogBook.png");
			ImageView imL = new ImageView(imgLog);
			DropShadow ds3 = new DropShadow();
			ds3.setOffsetY(5.0f);
			ds.setColor(Color.color(0.4f, 0.4f, 0.4f));
			imL.setEffect(ds3);

			//button for displaying next log 
			Button btnNext = new Button("Next \u276F"); // unicode char '>'
			btnNext.setPrefSize(80, 30);
			btnNext.setStyle(style4);
			btnNext.setEffect(ds);

			//Button for displaying previous log
			Button btnPrev = new Button("\u276E Prev"); // unicode char '<'
			btnPrev.setPrefSize(80, 30);
			btnPrev.setStyle(style4);
			btnPrev.setEffect(ds);

			//Button back for closing log and opening main screen 
			Button btnBack = new Button("Back");
			btnBack.setPrefSize(80, 30);
			btnBack.setStyle(style4);
			btnBack.setEffect(ds);
			
			// closing log stage
			btnBack.setOnAction(ee -> {
				stage.close();
			});
			// for log pane the top buttons and label
			HBox hbForTop = new HBox();
			hbForTop.setPadding(inSet);
			hbForTop.setSpacing(20);
			hbForTop.getChildren().addAll(btnPrev, imL, btnNext);
			hbForTop.setAlignment(Pos.CENTER);

			bP.setTop(hbForTop); // Main border pane in main screen

			GridPane gridForInfoL = new GridPane(); // Left log info
			gridForInfoL.setHgap(20);

			GridPane gridForInfoR = new GridPane();// Right log info
			gridForInfoR.setHgap(20);

			Label lblDate = new Label("Date");
			lblDate.setStyle(style);
			gridForInfoL.add(lblDate, 0, 0);
			Label lblDateInfo = new Label(user.logs.get(diveNo - 2).getDate().toString());
			lblDateInfo.setStyle(style4);
			gridForInfoL.add(lblDateInfo, 1, 0);

			Label lblName = new Label("Name");
			lblName.setStyle(style);
			gridForInfoL.add(lblName, 0, 1);
			Label lblNameInfo = new Label(name.toUpperCase());
			lblNameInfo.setStyle(style4);
			gridForInfoL.add(lblNameInfo, 1, 1);

			Label lblDiveNo = new Label("Dive #");
			lblDiveNo.setStyle(style);
			gridForInfoL.add(lblDiveNo, 0, 2);
			Label lblDiveInfo = new Label(String.valueOf(user.logs.get(diveNo - 2).getDiveNo()));
			lblDiveInfo.setStyle(style4);
			gridForInfoL.add(lblDiveInfo, 1, 2);

			Label lblLocation = new Label("Location");
			lblLocation.setStyle(style);
			gridForInfoL.add(lblLocation, 0, 3);
			Label lblLocationInfo = new Label();
			lblLocationInfo.setStyle(style4);
			lblLocationInfo.setText(user.logs.get(diveNo - 2).getLocation());
			gridForInfoL.add(lblLocationInfo, 1, 3);

			Label lblAirTemp = new Label("Air Temp");
			lblAirTemp.setStyle(style);
			gridForInfoL.add(lblAirTemp, 0, 4);
			Label lblAirTempInfo = new Label();
			lblAirTempInfo.setStyle(style4);
			lblAirTempInfo.setText(String.valueOf(user.logs.get(diveNo - 2).getAirTemp()) + "f");
			gridForInfoL.add(lblAirTempInfo, 1, 4);

			Label lblWaterTemp = new Label("Water Temp");
			lblWaterTemp.setStyle(style);
			gridForInfoL.add(lblWaterTemp, 0, 5);
			Label lblWaterTempInfo = new Label();
			lblWaterTempInfo.setStyle(style4);
			lblWaterTempInfo.setText(String.valueOf(user.logs.get(diveNo - 2).getWaterTemp()) + "f");
			gridForInfoL.add(lblWaterTempInfo, 1, 5);

			Label lblTankPS = new Label("Tank PSI Start");
			lblTankPS.setStyle(style);
			gridForInfoL.add(lblTankPS, 0, 6);
			Label lblTankPSInfo = new Label();
			lblTankPSInfo.setStyle(style4);
			lblTankPSInfo.setText(String.valueOf(user.logs.get(diveNo - 2).getTankStart()));
			gridForInfoL.add(lblTankPSInfo, 1, 6);

			Label lblTankPE = new Label("Tank PSI End");
			lblTankPE.setStyle(style);
			gridForInfoL.add(lblTankPE, 0, 7);
			Label lblTankPEInfo = new Label();
			lblTankPEInfo.setStyle(style4);
			lblTankPEInfo.setText(String.valueOf(user.logs.get(diveNo - 2).getTankEnd()));
			gridForInfoL.add(lblTankPEInfo, 1, 7);

			Label lblWeight = new Label("Weights");
			lblWeight.setStyle(style);
			gridForInfoL.add(lblWeight, 0, 8);
			Label lblWeightInfo = new Label();
			lblWeightInfo.setStyle(style4);
			lblWeightInfo.setText(String.valueOf(user.logs.get(diveNo - 2).getWeight()));
			gridForInfoL.add(lblWeightInfo, 1, 8);

			Label lblComments = new Label("Comments");
			lblComments.setStyle(style);
			gridForInfoL.add(lblComments, 0, 9);
			TextArea ta2Comments = new TextArea();
			ta2Comments.setStyle(style4);
			ta2Comments.setPrefWidth(280);
			ta2Comments.setPrefHeight(150);
			ta2Comments.setWrapText(true);
			ta2Comments.setEditable(false);
			ta2Comments.setText(user.logs.get(diveNo - 2).getComments());

			// VBox for comments
			VBox vbComments = new VBox();
			vbComments.setStyle(style4);
			vbComments.setMaxSize(280, 150);
			vbComments.getChildren().addAll(lblComments, ta2Comments);

			Label lblNone1 = new Label("None");
			CheckBox cbNone1 = new CheckBox();
			cbNone1.setDisable(true);
			gridForInfoL.add(lblNone1, 0, 10);
			gridForInfoL.add(cbNone1, 1, 10);
			lblNone1.setStyle(style);
			cbNone1.setSelected(user.logs.get(diveNo - 2).isNone());

			Label lblWetSuit1 = new Label("WetSuit");
			CheckBox cbWetSuit1 = new CheckBox();
			cbWetSuit1.setDisable(true);
			gridForInfoL.add(lblWetSuit1, 0, 11);
			gridForInfoL.add(cbWetSuit1, 1, 11);
			lblWetSuit1.setStyle(style);
			cbWetSuit1.setSelected(user.logs.get(diveNo - 2).isWetSuit());

			Label lblDrySuit1 = new Label("DrySuit");
			CheckBox cbDrySuit1 = new CheckBox();
			cbDrySuit1.setDisable(true);
			gridForInfoL.add(lblDrySuit1, 0, 12);
			gridForInfoL.add(cbDrySuit1, 1, 12);
			lblDrySuit1.setStyle(style);
			cbDrySuit1.setSelected(user.logs.get(diveNo - 2).isDrySuit());

			Label lblHood1 = new Label("Hood");
			CheckBox cbHood1 = new CheckBox();
			cbHood1.setDisable(true);
			gridForInfoL.add(lblHood1, 0, 13);
			gridForInfoL.add(cbHood1, 1, 13);
			lblHood1.setStyle(style);
			cbHood1.setSelected(user.logs.get(diveNo - 2).isHood());

			Label lblShorty1 = new Label("Shorty");
			CheckBox cbShorty1 = new CheckBox();
			cbShorty1.setDisable(true);
			gridForInfoL.add(lblShorty1, 0, 14);
			gridForInfoL.add(cbShorty1, 1, 14);
			lblShorty1.setStyle(style);
			cbShorty1.setSelected(user.logs.get(diveNo - 2).isShorty());

			Label lblGloves1 = new Label("Gloves");
			CheckBox cbGloves1 = new CheckBox();
			cbGloves1.setDisable(true);
			gridForInfoL.add(lblGloves1, 0, 15);
			gridForInfoL.add(cbGloves1, 1, 15);
			lblGloves1.setStyle(style);
			cbGloves1.setSelected(user.logs.get(diveNo - 2).isGloves());

			Label lblBoots1 = new Label("Boots");
			CheckBox cbBoots1 = new CheckBox();
			cbBoots1.setDisable(true);
			gridForInfoL.add(lblBoots1, 0, 16);
			gridForInfoL.add(cbBoots1, 1, 16);
			lblBoots1.setStyle(style);
			cbBoots1.setSelected(user.logs.get(diveNo - 2).isBoots());
			// ----------------------end left grid pane
			// ---------------------begin right grid pane
			Label lblSIHrs = new Label("Surface Interval Hrs");
			lblSIHrs.setStyle(style);
			gridForInfoR.add(lblSIHrs, 0, 0);
			Label lblSIHrsInfo = new Label();
			lblSIHrsInfo.setStyle(style4);
			lblSIHrsInfo.setText(String.valueOf(user.logs.get(diveNo - 2).getSurfaceIntervalHrs()));
			gridForInfoR.add(lblSIHrsInfo, 1, 0);

			Label lblSIMin = new Label("Surface Interval Min");
			lblSIMin.setStyle(style);
			gridForInfoR.add(lblSIMin, 0, 1);
			Label lblSIMinInfo = new Label();
			lblSIMinInfo.setStyle(style4);
			lblSIMinInfo.setText(String.valueOf(user.logs.get(diveNo - 2).getSurfaceIntervalMin()));
			gridForInfoR.add(lblSIMinInfo, 1, 1);

			Label lblPGStart = new Label("Pressure Group Start");
			lblPGStart.setStyle(style);
			gridForInfoR.add(lblPGStart, 0, 2);
			Label lblPGStartInfo = new Label();
			lblPGStartInfo.setStyle(style4);
			lblPGStartInfo.setText(user.logs.get(diveNo - 2).getPressureGroup());
			
			gridForInfoR.add(lblPGStartInfo, 1, 2);

			Label lblDepth = new Label("Depth");
			lblDepth.setStyle(style);
			gridForInfoR.add(lblDepth, 0, 3);
			Label lblDepthInfo = new Label();
			lblDepthInfo.setStyle(style4);
			lblDepthInfo.setText(String.valueOf(user.logs.get(diveNo - 2).getDepth()));
			gridForInfoR.add(lblDepthInfo, 1, 3);

			Label lblBT = new Label("Bottom Time Min");
			lblBT.setStyle(style);
			gridForInfoR.add(lblBT, 0, 4);
			Label lblBTInfo = new Label();
			lblBTInfo.setStyle(style4);
			lblBTInfo.setText(String.valueOf(user.logs.get(diveNo - 2).getBottomTime()));
			gridForInfoR.add(lblBTInfo, 1, 4);

			Label lblPGEnd = new Label("Pressure Group End");
			lblPGEnd.setStyle(style);
			gridForInfoR.add(lblPGEnd, 0, 5);
			Label lblPGEndInfo = new Label();
			lblPGEndInfo.setStyle(style4);
			lblPGEndInfo.setText(user.logs.get(diveNo - 2).getPressureGroup2());
			gridForInfoR.add(lblPGEndInfo, 1, 5);

			Label lblRNT = new Label("Residual Nitrogen Time");
			lblRNT.setStyle(style);
			gridForInfoR.add(lblRNT, 0, 6);
			Label lblRNTInfo = new Label();
			lblRNTInfo.setStyle(style4);
			lblRNTInfo.setText(user.logs.get(diveNo - 2).getResidualNitrogenTime());
			gridForInfoR.add(lblRNTInfo, 1, 6);

			Label lblABT = new Label("Actual Bottom Time");
			lblABT.setStyle(style);
			gridForInfoR.add(lblABT, 0, 7);
			Label lblABTInfo = new Label();
			lblABTInfo.setStyle(style4);
			lblABTInfo.setText(String.valueOf(user.logs.get(diveNo - 2).getBottomTime()));
			gridForInfoR.add(lblABTInfo, 1, 7);

			Label lblTBT = new Label("Total Bottom Time");
			lblTBT.setStyle(style);
			gridForInfoR.add(lblTBT, 0, 8);
			Label lblTBTInfo = new Label();
			lblTBTInfo.setStyle(style4);
			lblTBTInfo.setText(user.logs.get(diveNo - 2).getTotalBottomTime());
			gridForInfoR.add(lblTBTInfo, 1, 8);

			// check boxes for dive type
			Label lblFreshWater = new Label("Fresh Water");
			lblFreshWater.setStyle(style);
			gridForInfoR.add(lblFreshWater, 0, 9);
			CheckBox cbFreshWaterInfo = new CheckBox();
			cbFreshWaterInfo.setDisable(true);
			cbFreshWaterInfo.setStyle(style4);
			cbFreshWaterInfo.setSelected(user.logs.get(diveNo - 2).isFresh());
			gridForInfoR.add(cbFreshWaterInfo, 1, 9);

			Label lblSaltWater = new Label("Salt Water");
			lblSaltWater.setStyle(style);
			gridForInfoR.add(lblSaltWater, 0, 10);
			CheckBox cbSaltWaterInfo = new CheckBox();
			cbSaltWaterInfo.setDisable(true);
			cbSaltWaterInfo.setStyle(style4);
			cbSaltWaterInfo.setSelected(user.logs.get(diveNo - 2).isSalt());
			gridForInfoR.add(cbSaltWaterInfo, 1, 10);

			Label lblShore = new Label("Shore");
			lblShore.setStyle(style);
			gridForInfoR.add(lblShore, 0, 11);
			CheckBox cbShoreInfo = new CheckBox();
			cbShoreInfo.setDisable(true);
			cbShoreInfo.setStyle(style4);
			cbShoreInfo.setSelected(user.logs.get(diveNo - 2).isShore());
			gridForInfoR.add(cbShoreInfo, 1, 11);

			Label lblBoat = new Label("Boat");
			lblBoat.setStyle(style);
			gridForInfoR.add(lblBoat, 0, 12);
			CheckBox cbBoatInfo = new CheckBox();
			cbBoatInfo.setDisable(true);
			cbBoatInfo.setStyle(style4);
			cbBoatInfo.setSelected(user.logs.get(diveNo - 2).isBoat());
			gridForInfoR.add(cbBoatInfo, 1, 12);

			Label lblWaves = new Label("Waves");
			lblWaves.setStyle(style);
			gridForInfoR.add(lblWaves, 0, 13);
			CheckBox cbWavesInfo = new CheckBox();
			cbWavesInfo.setDisable(true);
			cbWavesInfo.setStyle(style4);
			cbWavesInfo.setSelected(user.logs.get(diveNo - 2).isWaves());
			gridForInfoR.add(cbWavesInfo, 1, 13);

			Label lblCurrent = new Label("Current");
			lblCurrent.setStyle(style);
			gridForInfoR.add(lblCurrent, 0, 14);
			CheckBox cbCurrentInfo = new CheckBox();
			cbCurrentInfo.setDisable(true);
			cbCurrentInfo.setStyle(style4);
			cbCurrentInfo.setSelected(user.logs.get(diveNo - 2).isCurrent());
			gridForInfoR.add(cbCurrentInfo, 1, 14);

			Label lblSurge = new Label("Surge");
			lblSurge.setStyle(style);
			gridForInfoR.add(lblSurge, 0, 15);
			CheckBox cbSurgeInfo = new CheckBox();
			cbSurgeInfo.setDisable(true);
			cbSurgeInfo.setStyle(style4);
			cbSurgeInfo.setSelected(user.logs.get(diveNo - 2).isSurge());
			gridForInfoR.add(cbSurgeInfo, 1, 15);
			// ----------end right grid pane

			HBox hbBottom = new HBox();
			hbBottom.setSpacing(10);
			hbBottom.getChildren().addAll(vbComments,btnBack);
			bP.setBottom(hbBottom); // comments text area set to bottom of the main pane
			hbBottom.setAlignment(Pos.BOTTOM_LEFT); // setting the button to the bottom of the hBox
			
			bP.setLeft(gridForInfoL);
			bP.setRight(gridForInfoR);

			stage.setResizable(false);
			Scene scene4 = new Scene(bP, 600, 700);
			stage.setScene(scene4);
			stage.setTitle("LOG");
			stage.show();

			// button Next displays the next log to log screen 
			btnNext.setOnAction(ee -> {

				// checking for over values in log list
				if (tempDiveNumber < diveNo - 2) {
					tempDiveNumber++; // incrementing log number
				}
				// setting log values
				lblDateInfo.setText(user.logs.get(tempDiveNumber).getDate().toString());
				lblNameInfo.setText(name.toUpperCase());
				lblDiveInfo.setText(String.valueOf(tempDiveNumber + 1));
				lblLocationInfo.setText(user.logs.get(tempDiveNumber).getLocation());
				lblAirTempInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getAirTemp()) + "f");
				lblWaterTempInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getWaterTemp()) + 'f');
				lblTankPSInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getTankStart()));
				lblTankPEInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getTankEnd()));
				lblWeightInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getWeight()));
				ta2Comments.setText(user.logs.get(tempDiveNumber).getComments());
				cbNone1.setSelected(user.logs.get(tempDiveNumber).isNone());
				cbWetSuit1.setSelected(user.logs.get(tempDiveNumber).isWetSuit());
				cbDrySuit1.setSelected(user.logs.get(tempDiveNumber).isDrySuit());
				cbHood1.setSelected(user.logs.get(tempDiveNumber).isHood());
				cbShorty1.setSelected(user.logs.get(tempDiveNumber).isShorty());
				cbGloves1.setSelected(user.logs.get(tempDiveNumber).isGloves());
				cbBoots1.setSelected(user.logs.get(tempDiveNumber).isBoots());
				lblSIHrsInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getSurfaceIntervalHrs()) + "     ");
				lblSIMinInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getSurfaceIntervalMin()));
				lblPGStartInfo.setText(user.logs.get(tempDiveNumber).getPressureGroup());
				lblDepthInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getDepth()));
				lblBTInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getBottomTime()));
				lblPGEndInfo.setText(user.logs.get(tempDiveNumber).getPressureGroup2());
				lblRNTInfo.setText(user.logs.get(tempDiveNumber).getResidualNitrogenTime());
				lblABTInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getBottomTime()));
				lblTBTInfo.setText(user.logs.get(tempDiveNumber).getTotalBottomTime());
				cbFreshWaterInfo.setSelected(user.logs.get(tempDiveNumber).isFresh());
				cbSaltWaterInfo.setSelected(user.logs.get(tempDiveNumber).isSalt());
				cbShoreInfo.setSelected(user.logs.get(tempDiveNumber).isShore());
				cbBoatInfo.setSelected(user.logs.get(tempDiveNumber).isBoat());
				cbWavesInfo.setSelected(user.logs.get(tempDiveNumber).isWaves());
				cbCurrentInfo.setSelected(user.logs.get(tempDiveNumber).isCurrent());
				cbSurgeInfo.setSelected(user.logs.get(tempDiveNumber).isSurge());
			});
			// button previous displays the previous log to the screen
			btnPrev.setOnAction(ee -> {

				// making log number stop at 1
				if (tempDiveNumber > 0) {
					--tempDiveNumber; // decrementing log number
				}
				// setting log values
				lblDateInfo.setText(user.logs.get(tempDiveNumber).getDate().toString());
				lblNameInfo.setText(name.toUpperCase());
				lblDiveInfo.setText(String.valueOf(tempDiveNumber + 1));
				lblLocationInfo.setText(user.logs.get(tempDiveNumber).getLocation());
				lblAirTempInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getAirTemp()) + "f");
				lblWaterTempInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getWaterTemp()) + 'f');
				lblTankPSInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getTankStart()));
				lblTankPEInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getTankEnd()));
				lblWeightInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getWeight()));
				ta2Comments.setText(user.logs.get(tempDiveNumber).getComments());
				cbNone1.setSelected(user.logs.get(tempDiveNumber).isNone());
				cbWetSuit1.setSelected(user.logs.get(tempDiveNumber).isWetSuit());
				cbDrySuit1.setSelected(user.logs.get(tempDiveNumber).isDrySuit());
				cbHood1.setSelected(user.logs.get(tempDiveNumber).isHood());
				cbShorty1.setSelected(user.logs.get(tempDiveNumber).isShorty());
				cbGloves1.setSelected(user.logs.get(tempDiveNumber).isGloves());
				cbBoots1.setSelected(user.logs.get(tempDiveNumber).isBoots());
				lblSIHrsInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getSurfaceIntervalHrs()) + "     ");
				lblSIMinInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getSurfaceIntervalMin()));
				lblPGStartInfo.setText(user.logs.get(tempDiveNumber).getPressureGroup());
				lblDepthInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getDepth()));
				lblBTInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getBottomTime()));
				lblPGEndInfo.setText(user.logs.get(tempDiveNumber).getPressureGroup2());
				lblRNTInfo.setText(user.logs.get(tempDiveNumber).getResidualNitrogenTime());
				lblABTInfo.setText(String.valueOf(user.logs.get(tempDiveNumber).getBottomTime()));
				lblTBTInfo.setText(user.logs.get(tempDiveNumber).getTotalBottomTime());
				cbFreshWaterInfo.setSelected(user.logs.get(tempDiveNumber).isFresh());
				cbSaltWaterInfo.setSelected(user.logs.get(tempDiveNumber).isSalt());
				cbShoreInfo.setSelected(user.logs.get(tempDiveNumber).isShore());
				cbBoatInfo.setSelected(user.logs.get(tempDiveNumber).isBoat());
				cbWavesInfo.setSelected(user.logs.get(tempDiveNumber).isWaves());
				cbCurrentInfo.setSelected(user.logs.get(tempDiveNumber).isCurrent());
				cbSurgeInfo.setSelected(user.logs.get(tempDiveNumber).isSurge());
			});
			primaryStage.setIconified(true);

			// centering the main stage in the screen
			Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
			stage.setX((primScreenBounds.getWidth() - stage.getWidth()) / 2);
			stage.setY((primScreenBounds.getHeight() - stage.getHeight()) / 4);

			// when the main stage closes
			stage.setOnHiding(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent event) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {

							// reopening the primaryStage
							primaryStage.setIconified(false);

						} // end run
					}); // end run later
				}
			}); // end set on hiding
		});

		//Button Save saves the log to the user.log array
		Button btnSave = new Button("Save Log");
		btnSave.setPrefSize(80, 30);
		btnSave.setStyle(style4);
		btnSave.setEffect(ds);
		
		// Bottom of main screen
		HBox bottom = new HBox();
		bottom.setPadding(inSet);
		bottom.setSpacing(20);
		bottom.setAlignment(Pos.CENTER);
		bottom.getChildren().add(btnSave);
		pane.setBottom(bottom);

		// VBox for left side log info
		// each label and info label is in a HBox inside the VBox
		// each info label is set to default values
		VBox infoPaneLeft = new VBox();
		infoPaneLeft.setPadding(inSet);
		infoPaneLeft.setSpacing(10);

		// Date
		HBox hBoxDate = new HBox();
		hBoxDate.setSpacing(50);
		Date date = new Date();
		Label lblDate1 = new Label("Date:");
		lblDate1.setStyle(style);
		Label lblDate = new Label(date.toString());
		lblDate.setStyle(style4);
		hBoxDate.getChildren().addAll(lblDate1, lblDate);

		// Dive number
		HBox hBoxDiveNo = new HBox();
		hBoxDiveNo.setSpacing(5);

		Label lblDiveNo = new Label("Dive # ");
		// setting dive no
		diveNo = user.logs.size() + 1;
		Label tfDiveNo = new Label(String.valueOf(diveNo)); // sets dive number to size of logs list

		lblDiveNo.setStyle(style);
		tfDiveNo.setStyle(style2);
		hBoxDiveNo.getChildren().addAll(lblDiveNo, tfDiveNo);

		HBox hBoxName = new HBox();
		hBoxDiveNo.setSpacing(5);

		Label lblName = new Label("Name ");
		Label tfName = new Label(name.toUpperCase());
		lblName.setStyle(style);
		tfName.setStyle(style2);
		hBoxName.getChildren().addAll(lblName, tfName);

		HBox hbNameDiveNo = new HBox();
		hbNameDiveNo.setSpacing(110);
		hbNameDiveNo.getChildren().addAll(hBoxName, hBoxDiveNo);

		// Location
		HBox hBoxLocation = new HBox();
		Label lblLocation = new Label("Location");
		lblLocation.setStyle(style);

		TextField tfLocation = new TextField();
	//	tfLocation.setBackground(pane.getBackground()); // sets screen to background color	
		tfLocation.setStyle(style4);  // ads style and underlines text field
		
		hBoxLocation.setSpacing(34);
		hBoxLocation.getChildren().addAll(lblLocation, tfLocation);

		// Temperature
		HBox hBoxTemp = new HBox();
		hBoxTemp.setStyle(style);
		hBoxTemp.setSpacing(5);

		Label lblAirTemp = new Label("Air Temp \n\t" + 80 + "f");
		lblAirTemp.setStyle(style);

		Label lblWaterTemp = new Label("Water Temp \n\t     " + 60 + "f");
		lblWaterTemp.setStyle(style);

		Slider sliderAir = new Slider();
		sliderAir.setMin(0);
		sliderAir.setMax(100);
		sliderAir.setValue(80);
		sliderAir.setShowTickLabels(true);
		sliderAir.setShowTickMarks(true);
		sliderAir.setBlockIncrement(2);
		sliderAir.setOrientation(Orientation.VERTICAL);
		sliderAir.setMajorTickUnit(10);
		sliderAir.setMinorTickCount(1);

		// Adding Listener to Air Slider value property.
		sliderAir.valueProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, //
					Number oldValue, Number newValue) {

				lblAirTemp.setText(("Air Temp \n\t" + newValue.intValue()) + "f"); // casting to integer
			}
		});

		Slider sliderWater = new Slider();
		sliderWater.setMin(0);
		sliderWater.setMax(100);
		sliderWater.setValue(60);
		sliderWater.setShowTickLabels(true);
		sliderWater.setShowTickMarks(true);
		sliderWater.setBlockIncrement(2);
		sliderWater.setOrientation(Orientation.VERTICAL);
		sliderWater.setMajorTickUnit(10);
		sliderWater.setMinorTickCount(1);

		// Adding Listener to value property for slider water.
		sliderWater.valueProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				lblWaterTemp.setText(("Water Temp \n\t     " + newValue.intValue()) + "f"); // casting to integer
			}
		});

		// H box for air pressure in tank
		HBox hBoxTankPsi = new HBox();
		hBoxTemp.setStyle(style);
		hBoxTemp.setSpacing(7);

		Label lblTankPsiStart = new Label("Tank\nStart PSI    \n" + 3000 + " PSI");
		lblTankPsiStart.setStyle(style);

		Slider sliderTankPsiStart = new Slider();
		sliderTankPsiStart.setMin(0);
		sliderTankPsiStart.setMax(5000);
		sliderTankPsiStart.setValue(3000);
		sliderTankPsiStart.setShowTickLabels(true);
		sliderTankPsiStart.setShowTickMarks(true);
		sliderTankPsiStart.setBlockIncrement(10);
		sliderTankPsiStart.setOrientation(Orientation.VERTICAL);
		sliderTankPsiStart.setMajorTickUnit(100);
		sliderTankPsiStart.setMinorTickCount(10);
		sliderTankPsiStart.setSnapToTicks(true);

		// Adding Listener to value property for slider tank pressure start .
		sliderTankPsiStart.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				lblTankPsiStart.setText(("Tank\nStart PSI    \n" + newValue.intValue()) + " PSI"); // casting to integer
			}
		});

		Label lblTankPsiEnd = new Label("\t   Tank\n\t   End PSI  \n\t   " + 700 + " PSI");
		lblTankPsiEnd.setStyle(style);

		Slider sliderTankPsiEnd = new Slider();
		sliderTankPsiEnd.setMin(0);
		sliderTankPsiEnd.setMax(5000);
		sliderTankPsiEnd.setValue(700);
		sliderTankPsiEnd.setShowTickLabels(true);
		sliderTankPsiEnd.setShowTickMarks(true);
		sliderTankPsiEnd.setBlockIncrement(10);
		sliderTankPsiEnd.setOrientation(Orientation.VERTICAL);
		sliderTankPsiEnd.setMajorTickUnit(100);
		sliderTankPsiEnd.setMinorTickCount(10);
		sliderTankPsiEnd.setSnapToTicks(true);

		// Adding Listener to value property slider tank end.
		sliderTankPsiEnd.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				lblTankPsiEnd.setText(("\t   Tank\n\t   End PSI  \n \t   " + newValue.intValue()) + " PSI"); // casting
																												// to
																												// integer
			}
		});

		// VBox for dive types labels and checkBoxes
		HBox hbCheckBoxes = new HBox();
		hbCheckBoxes.setSpacing(90);

		GridPane diveCheckBoxL = new GridPane();
		diveCheckBoxL.setStyle(style4);
		diveCheckBoxL.setHgap(5);
		diveCheckBoxL.setVgap(5);

		GridPane diveCheckBoxR = new GridPane();
		diveCheckBoxR.setStyle(style4);
		diveCheckBoxR.setHgap(5);
		diveCheckBoxR.setVgap(5);

		Label lblFresh = new Label("Fresh");
		CheckBox cbFresh = new CheckBox();
		diveCheckBoxL.add(lblFresh, 0, 0);
		diveCheckBoxL.add(cbFresh, 1, 0);
		lblFresh.setStyle(style);

		Label lblSalt = new Label("Salt");
		CheckBox cbSalt = new CheckBox();
		diveCheckBoxL.add(lblSalt, 0, 1);
		diveCheckBoxL.add(cbSalt, 1, 1);
		lblSalt.setStyle(style);

		Label lblShore = new Label("Shore");
		CheckBox cbShore = new CheckBox();
		diveCheckBoxL.add(lblShore, 0, 2);
		diveCheckBoxL.add(cbShore, 1, 2);
		lblShore.setStyle(style);

		Label lblBoat = new Label("Boat");
		CheckBox cbBoat = new CheckBox();
		diveCheckBoxL.add(lblBoat, 0, 3);
		diveCheckBoxL.add(cbBoat, 1, 3);
		lblBoat.setStyle(style);

		Label lblWaves = new Label("Waves");
		CheckBox cbWaves = new CheckBox();
		diveCheckBoxL.add(lblWaves, 0, 4);
		diveCheckBoxL.add(cbWaves, 1, 4);
		lblWaves.setStyle(style);

		Label lblCurrent = new Label("Current");
		CheckBox cbCurrent = new CheckBox();
		diveCheckBoxL.add(lblCurrent, 0, 5);
		diveCheckBoxL.add(cbCurrent, 1, 5);
		lblCurrent.setStyle(style);

		Label lblSurge = new Label("Surge");
		CheckBox cbSurge = new CheckBox();
		diveCheckBoxL.add(lblSurge, 0, 6);
		diveCheckBoxL.add(cbSurge, 1, 6);
		lblSurge.setStyle(style);

		// Exposure check boxes
		Label lblNone = new Label("None");
		CheckBox cbNone = new CheckBox();
		diveCheckBoxR.add(lblNone, 0, 0);
		diveCheckBoxR.add(cbNone, 1, 0);
		lblNone.setStyle(style);

		Label lblWetSuit = new Label("WetSuit");
		CheckBox cbWetSuit = new CheckBox();
		diveCheckBoxR.add(lblWetSuit, 0, 1);
		diveCheckBoxR.add(cbWetSuit, 1, 1);
		lblWetSuit.setStyle(style);

		Label lblDrySuit = new Label("DrySuit");
		CheckBox cbDrySuit = new CheckBox();
		diveCheckBoxR.add(lblDrySuit, 0, 2);
		diveCheckBoxR.add(cbDrySuit, 1, 2);
		lblDrySuit.setStyle(style);

		Label lblHood = new Label("Hood");
		CheckBox cbHood = new CheckBox();
		diveCheckBoxR.add(lblHood, 0, 3);
		diveCheckBoxR.add(cbHood, 1, 3);
		lblHood.setStyle(style);

		Label lblShorty = new Label("Shorty");
		CheckBox cbShorty = new CheckBox();
		diveCheckBoxR.add(lblShorty, 0, 4);
		diveCheckBoxR.add(cbShorty, 1, 4);
		lblShorty.setStyle(style);

		Label lblGloves = new Label("Gloves");
		CheckBox cbGloves = new CheckBox();
		diveCheckBoxR.add(lblGloves, 0, 5);
		diveCheckBoxR.add(cbGloves, 1, 5);
		lblGloves.setStyle(style);

		Label lblBoots = new Label("Boots");
		CheckBox cbBoots = new CheckBox();
		diveCheckBoxR.add(lblBoots, 0, 6);
		diveCheckBoxR.add(cbBoots, 1, 6);
		lblBoots.setStyle(style);

		hbCheckBoxes.getChildren().addAll(diveCheckBoxL, diveCheckBoxR);

		// grid pane for dive information inputs right
		VBox infoPaneRight = new VBox();
		infoPaneRight.setPadding(inSet);
		infoPaneRight.setSpacing(5);

		// Comments text area
		TextArea taComments = new TextArea();
		Label lblComents = new Label("Comments");
		lblComents.setStyle(style);	
		taComments.blendModeProperty();
		taComments.setStyle(style); 
	//	taComments.getStylesheets().add("taStyle.css"); //style sheets for making the text area clear
		
		taComments.setPrefHeight(100);
		taComments.setPrefWidth(280);
		taComments.setWrapText(true);
		

		// Pane for the dive profile layout to get dive depth and time
		Pane divePane = new Pane();
		divePane.setPadding(new Insets(10, 10, 10, 10));
		divePane.setPrefSize(280, 190);

		// text field for surface interval
		Label lblSurfaceInterval = new Label("Surface Interval");
		lblSurfaceInterval.setStyle(style5);
		lblSurfaceInterval.setLayoutY(15);
		lblSurfaceInterval.setLayoutX(0);

		TextField tfSurfaceInterval = new TextField();
		tfSurfaceInterval.setStyle(style3);
		tfSurfaceInterval.setEditable(false);
		tfSurfaceInterval.setFocusTraversable(false);
		tfSurfaceInterval.setPrefWidth(60);
		tfSurfaceInterval.setPromptText("S.I.");
		tfSurfaceInterval.setLayoutX(0);
		tfSurfaceInterval.setLayoutY(30);

		// pressure group1
		Label lblPressureGroup1 = new Label("Starting\nPressure Group");
		lblPressureGroup1.setStyle(style5);
		lblPressureGroup1.setLayoutY(0);
		lblPressureGroup1.setLayoutX(90);

		TextField tfPressureGroup1 = new TextField();
		tfPressureGroup1.setStyle(style3);
		tfPressureGroup1.setEditable(false);
		tfPressureGroup1.setFocusTraversable(false);
		tfPressureGroup1.setText("A");
		tfPressureGroup1.setPrefWidth(60);
		tfPressureGroup1.setLayoutX(90);
		tfPressureGroup1.setLayoutY(30);

		// tfPressureGroup2 is the second pressure group calculated from total bottom
		// time
		Label lblPressureGroup2 = new Label("Ending\nPressure Group");
		lblPressureGroup2.setStyle(style5);
		lblPressureGroup2.setLayoutY(0);
		lblPressureGroup2.setLayoutX(195);

		TextField tfPressureGroup2 = new TextField();
		tfPressureGroup2.setStyle(style3);
		tfPressureGroup2.setEditable(false);// set to false just a display only
		tfPressureGroup2.setFocusTraversable(false);
		tfPressureGroup2.setPrefColumnCount(4);
		tfPressureGroup2.setPromptText("P.G.");
		tfPressureGroup2.setPrefWidth(60);
		tfPressureGroup2.setLayoutX(195);
		tfPressureGroup2.setLayoutY(30);

		// tfDeph is the for enter dive depth
		Label lblDepthMini = new Label("Depth");
		lblDepthMini.setStyle(style5);
		lblDepthMini.setLayoutY(85);
		lblDepthMini.setLayoutX(35);

		TextField tfDepth = new TextField();
		tfDepth.setStyle(style3);
		tfDepth.setEditable(false);
		tfDepth.setFocusTraversable(false);
		tfDepth.setPrefColumnCount(4);
		tfDepth.setPromptText("Depth");
		tfDepth.setPrefWidth(58);
		tfDepth.setLayoutX(35);
		tfDepth.setLayoutY(100);

		// tfBottomTime for bottom time
		Label lblBottomTimeMini = new Label("Bottom Time\nActual");
		lblBottomTimeMini.setStyle(style5);
		lblBottomTimeMini.setLayoutY(115);
		lblBottomTimeMini.setLayoutX(105);
		TextField tfBottomTime = new TextField();
		tfBottomTime.setStyle(style3);
		tfBottomTime.setEditable(false);
		tfBottomTime.setFocusTraversable(false);
		tfBottomTime.setPrefColumnCount(4);
		tfBottomTime.setPromptText("B.T.");
		tfBottomTime.setPrefWidth(60);
		tfBottomTime.setLayoutX(105);
		tfBottomTime.setLayoutY(145);

		// tfBottomTime for bottom time total
		Label lblBottomTimeTotalMini = new Label("Bottom Time\nTotal");
		lblBottomTimeTotalMini.setStyle(style5);
		lblBottomTimeTotalMini.setLayoutY(115);
		lblBottomTimeTotalMini.setLayoutX(175);

		TextField tfBottomTimeTotal = new TextField();
		tfBottomTimeTotal.setStyle(style3);
		tfBottomTimeTotal.setEditable(false);
		tfBottomTimeTotal.setFocusTraversable(false);
		tfBottomTimeTotal.setPrefColumnCount(4);
		tfBottomTimeTotal.setPromptText("B.T.");
		tfBottomTimeTotal.setPrefWidth(60);
		tfBottomTimeTotal.setLayoutX(175);
		tfBottomTimeTotal.setLayoutY(145);

		// Lines are drawn to pane just to look like a dive profile
		Line line1 = new Line();
		line1.setStrokeWidth(3);
		line1.setStartY(60);
		line1.setEndY(60);
		line1.setStartX(0);
		line1.setEndX(105);

		Line line2 = new Line();
		line2.setStrokeWidth(3);
		line2.setStartY(60);
		line2.setEndY(110);
		line2.setStartX(105);
		line2.setEndX(100);

		Line line3 = new Line();
		line3.setStrokeWidth(3);
		line3.setStartY(110);
		line3.setEndY(110);
		line3.setStartX(100);
		line3.setEndX(190);

		Line line4 = new Line();
		line4.setStrokeWidth(3);
		line4.setStartY(110);
		line4.setEndY(60);
		line4.setStartX(190);
		line4.setEndX(195);

		Line line5 = new Line();
		line5.setStrokeWidth(3);
		line5.setStartY(60);
		line5.setEndY(60);
		line5.setStartX(195);
		line5.setEndX(255);

		// spinners for planner input
		HBox hbHours = new HBox();
		hbHours.setSpacing(48);

		Label lblHours = new Label("Surface Interval Hrs ");
		lblHours.setStyle(style);

		Spinner<Integer> spinnerHrs = new Spinner<>();
		spinnerHrs.setPrefWidth(60);
		spinnerHrs.getEditor().setStyle(style3);
		
		
		SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 12, 0);
		spinnerHrs.setValueFactory(valueFactory);

		hbHours.getChildren().addAll(lblHours, spinnerHrs);

		// spinner for hours
		HBox hbMin = new HBox();
		hbMin.setSpacing(45);

		Label lblMin = new Label("Surface Interval Min ");
		lblMin.setStyle(style);

		Spinner<Integer> spinnerMin = new Spinner<>();
		spinnerMin.setPrefWidth(60);
		spinnerMin.getEditor().setStyle(style3);
		SpinnerValueFactory<Integer> valueFactory3 = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60, 0);
		spinnerMin.setValueFactory(valueFactory3);

		hbMin.getChildren().addAll(lblMin, spinnerMin);

		// Depth
		HBox hbDepth = new HBox();
		hbDepth.setSpacing(133);

		Label lblDepth = new Label("Depth Ft ");
		lblDepth.setStyle(style);

		Spinner<Integer> spinnerDepth = new Spinner<>();
		spinnerDepth.setPrefWidth(60);
		spinnerDepth.getEditor().setStyle(style3);
		
		SpinnerValueFactory<Integer> valueFactory4 = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 140, 0);
		spinnerDepth.setValueFactory(valueFactory4);

		hbDepth.getChildren().addAll(lblDepth, spinnerDepth);

		// Bottom time
		HBox hbBottomTm = new HBox();
		hbBottomTm.setSpacing(58);

		Label lblBottomTm = new Label("Bottom Time  MIN ");
		lblBottomTm.setStyle(style);

		Spinner<Integer> spinnerBottomTm = new Spinner<>();
		spinnerBottomTm.setPrefWidth(60);
		spinnerBottomTm.getEditor().setStyle(style3);
		// set to 205 minutes
		SpinnerValueFactory<Integer> valueFactory5 = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 205, 0);
		spinnerBottomTm.setValueFactory(valueFactory5);

		hbBottomTm.getChildren().addAll(lblBottomTm, spinnerBottomTm);

		// Weight used
		HBox hbWeight = new HBox();
		hbWeight.setSpacing(90);

		Label lblWeight = new Label("Weight Used lbs  ");
		lblWeight.setStyle(style);

		Spinner<Integer> spinnerWeight = new Spinner<>();
		spinnerWeight.setPrefWidth(65);
		spinnerWeight.getEditor().setStyle(style3); // set style to the spinner display
		SpinnerValueFactory<Integer> valueFactory6 = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 0);
		spinnerWeight.setValueFactory(valueFactory6);
		hbWeight.getChildren().addAll(lblWeight, spinnerWeight);
		// When spinner change value.
		spinnerWeight.valueProperty().addListener(new ChangeListener<Integer>() {

			@Override
			public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {

			}
		});

		// hBox for display total bottom time and total nitrogen time
		VBox vbDisplay = new VBox();

		// HBox for total nitrogen time
		HBox hbRN = new HBox();
		hbRN.setSpacing(29);
		Label lblRisidualNitrogen = new Label("Risidual Nitrogen Time");
		lblRisidualNitrogen.setStyle(style);
		Label lblRn = new Label("N/A");
		lblRn.setStyle(style3);

		// For current residual nitrogen time
		// HBox for total nitrogen time
		HBox hbRN2 = new HBox();
		hbRN2.setSpacing(8);

		Label lblRisidualNitrogen2 = new Label("Current Risidual Nitrogen");
		lblRisidualNitrogen2.setStyle(style);

		Label lblRn2 = new Label("N/A");
		lblRn2.setStyle(style3);

		// hBox for actual bottom time
		HBox hbABT = new HBox();
		hbABT.setSpacing(51);

		Label lblActualBottomTime = new Label("Actual Bottom Time");
		lblActualBottomTime.setStyle(style);

		Label lblABT = new Label("N/A");
		lblABT.setStyle(style3);

		// hbox for total bottom time
		HBox hbBottomTime = new HBox();
		hbBottomTime.setSpacing(62);

		Label lblTotalBottomTime = new Label("Total Bottom Time");
		lblTotalBottomTime.setStyle(style);

		Label lblBT = new Label();
		lblBT.setText("N/A");
		lblBT.setStyle(style3);

		HBox hbPressureGroup = new HBox();
		hbPressureGroup.setSpacing(83);

		Label lblPressureGroup = new Label("Pressure Group ");
		lblPressureGroup.setStyle(style);

		Label lblPG = new Label("N/A");
		lblPG.setStyle(style3);

		// spinners action set after to call values of other spinners
		// When spinner hours change value.
		spinnerHrs.valueProperty().addListener(new ChangeListener<Integer>() {
			@Override
			public void changed(ObservableValue<? extends Integer> observable, //
					Integer oldValue, Integer newValue) {

				tfSurfaceInterval.setText(newValue + ":" + spinnerMin.getValue());

			}
		});

		Label lblNewPG = new Label("N/A");
		lblNewPG.setStyle(style4);

		// When spinner minute change value.
		spinnerMin.valueProperty().addListener(new ChangeListener<Integer>() {
			@Override
			public void changed(ObservableValue<? extends Integer> observable, //
					Integer oldValue, Integer newValue) {

				tfSurfaceInterval.setText(spinnerHrs.getValue() + ":" + newValue);

				// if statement to check if save plan is pressed without a first dive pressure
				// group entered
				if (!lblPG.getText().equals("N/A")) {
					lblNewPG.setText("" + calculateNewPressureGroup(lblPG.getText().toCharArray()[0],
							Integer.valueOf(spinnerHrs.getValue()), Integer.valueOf(spinnerMin.getValue())));
				} else {
					lblNewPG.setText("" + calculateNewPressureGroup('A', Integer.valueOf(spinnerHrs.getValue()),
							Integer.valueOf(spinnerMin.getValue())));
				}
			}
		});

		// When spinner depth change value.
		spinnerDepth.valueProperty().addListener(new ChangeListener<Integer>() {

			@Override
			public void changed(ObservableValue<? extends Integer> observable, //
					Integer oldValue, Integer newValue) {

				tfDepth.setText(String.valueOf(newValue));
				// System.out.println(newValue + " Depth");

			}
		});
		// When spinner bottom time change value.
		spinnerBottomTm.valueProperty().addListener(new ChangeListener<Integer>() {

			@Override
			public void changed(ObservableValue<? extends Integer> observable, //
					Integer oldValue, Integer newValue) {

				tfBottomTime.setText(String.valueOf(newValue));

				// setting the text field total bottom time only if planner has set residual
				// nitrogen time
				if (lblRn.getText() != "N/A") {
					tfBottomTimeTotal.setText(
							(Integer.parseInt(tfBottomTime.getText()) + (Integer.parseInt(lblRn.getText()))) + "");
					// setting the label
					lblBT.setText(tfBottomTimeTotal.getText());
				}

				// setting actual bottom time
				lblABT.setText(newValue.toString());

				if (tfBottomTimeTotal.getText().isEmpty()) {
					tfPressureGroup2.setText("" + calculateFirstDivePressureGroup(Integer.valueOf(tfDepth.getText()),
							Integer.valueOf(tfBottomTime.getText())));
				} else {
					tfPressureGroup2.setText("" + calculateFirstDivePressureGroup(Integer.valueOf(tfDepth.getText()),
							Integer.valueOf(tfBottomTimeTotal.getText())));
				}

				lblPG.setText(tfPressureGroup2.getText()); // setting pressure group Starting

				// Checking for over value in bottom time
				if (checkBottomTimeMax(Integer.parseInt(tfDepth.getText()),
						Integer.parseInt(tfBottomTime.getText())) == '!') {

					// to calculate next dive
					spinnerBottomTm.getValueFactory().decrement(1); // sets the max value to the bottom time spinner
				}

				// Checking for over value in total bottom time, for second dive with residual
				// and actual bottom time
				if (!tfBottomTimeTotal.getText().isEmpty()) {
					if (checkBottomTimeMax(Integer.parseInt(tfDepth.getText()),
							Integer.parseInt(tfBottomTimeTotal.getText())) == '!') {
						spinnerBottomTm.getValueFactory().decrement(1);
					}
				}
			}

		});

		// button save saves a Log to user.log
		btnSave.setOnAction(e -> {

			// creating a log object
			Log log = new Log();
			log.setName(name);
			log.setDate(new Date());
			log.setDiveNo(diveNo++);
			tfDiveNo.setText(String.valueOf(diveNo));
			log.setLocation(tfLocation.getText().trim());
			log.setAirTemp((int) sliderAir.getValue());
			log.setWaterTemp((int) sliderWater.getValue());
			log.setTankStart((int) sliderTankPsiStart.getValue());
			log.setTankEnd((int) sliderTankPsiEnd.getValue());
			log.setWeight((int) spinnerWeight.getValue());
			log.setComments(taComments.getText());
			log.setFresh(cbFresh.isSelected());
			log.setSalt(cbSalt.isSelected());
			log.setShore(cbShore.isSelected());
			log.setBoat(cbBoat.isSelected());
			log.setWaves(cbWaves.isSelected());
			log.setCurrent(cbCurrent.isSelected());
			log.setSurge(cbSurge.isSelected());
			log.setNone(cbNone.isSelected());
			log.setWetSuit(cbWetSuit.isSelected());
			log.setDrySuit(cbDrySuit.isSelected());
			log.setHood(cbHood.isSelected());
			log.setShorty(cbShorty.isSelected());
			log.setGloves(cbGloves.isSelected());
			log.setBoots(cbBoots.isSelected());

			log.setSurfaceIntervalHrs(spinnerHrs.getValue());
			log.setSurfaceIntervalMin(spinnerMin.getValue());
			log.setDepth(spinnerDepth.getValue());
			log.setBottomTime(spinnerBottomTm.getValue());

			if (!tfPressureGroup1.getText().equals("N/A")) {
				log.setPressureGroup(tfPressureGroup1.getText());
			}
			if (!tfPressureGroup2.getText().equals("N/A")) {
				log.setPressureGroup2(tfPressureGroup2.getText());
			}
			log.setResidualNitrogenTime(lblRn.getText());
			log.setTotalBottomTime(lblBT.getText());

			// adding a log to the logs arrayList of Logs
			user.logs.add(log);

			createPopup(); // pop up for file saved feedback

			// System.out.println(user.logs.toString()); // prints out all logs TEST
		});

		hbRN.getChildren().addAll(lblRisidualNitrogen, lblRn);
		hbRN2.getChildren().addAll(lblRisidualNitrogen2, lblRn2);
		hbABT.getChildren().addAll(lblActualBottomTime, lblABT);
		hbBottomTime.getChildren().addAll(lblTotalBottomTime, lblBT);
		hbPressureGroup.getChildren().addAll(lblPressureGroup, lblPG);

		vbDisplay.getChildren().addAll(hbRN, hbABT, hbBottomTime, hbPressureGroup);

		// Planner Button
		btnPlanner.setOnAction(e -> {

			// Saving temp values to return to main button return to main sets these values back to main screen 
			String tempLblRn = lblRn.getText();
			String templblAbt =lblABT.getText();
			String templblBottomTime = lblBT.getText();
			String tempPressureGroup = lblPG.getText();
			String tempSI = tfSurfaceInterval.getText();
			
			// if statements for checking blank values and setting them 
			if (tfPressureGroup2.getText().equals("")) {
				lblPG.setText("A");
			}
			if (tfDepth.getText().isEmpty() || tfDepth.getText().equals("")) {
				tfDepth.setText("0");
			}
			if(tfBottomTime.getText().equals("")) {
				tfBottomTime.setText("0");
			}
			
			// new stage for a new window to plan a second dive
			Stage stage = new Stage();

			// mainHbox is for info in the right side of the planner screen
			HBox mainHbox = new HBox();

			// set current residual nitrogen
			rNt = 0;

			// checking for pressure group of A or N/A
			if (lblPressureGroup.getText() != "N/A") {
				calculateMaxDiveTime('A', Integer.valueOf(tfDepth.getText()));

			} else {
				calculateMaxDiveTime(lblPressureGroup.getText().toCharArray()[0], Integer.valueOf(tfDepth.getText()));
			}
			// setting ending pressure group
			lblRn2.setText(String.valueOf(rNt));

			// Image of planner for planner screen
			Image imPlan = new Image("pics/planner.jpg", 100, 60, true, true);
			ImageView imVPlan = new ImageView(imPlan);
			DropShadow ds4 = new DropShadow();
			ds4.setOffsetY(5.0f);
			ds4.setColor(Color.color(0.4f, 0.4f, 0.4f));
			imVPlan.setEffect(ds4);
			
			// setting imagein a label
			VBox vbLabel = new VBox();
			vbLabel.setAlignment(Pos.CENTER);
			vbLabel.getChildren().add(imVPlan);

			// Main planner pane
			Pane panePlanner = new Pane();

			VBox leftVbox = new VBox();
			leftVbox.setPadding(inSet);
			leftVbox.setSpacing(10);
			VBox vb = new VBox();
			vb.setSpacing(10);

			VBox rightVbox = new VBox();
			rightVbox.setPadding(inSet);
			rightVbox.setSpacing(10);

			HBox hbSlider = new HBox();
			hbSlider.setSpacing(50);

			Label lblDepthPlanner = new Label("Plan Depth \n30Ft ");
			lblDepthPlanner.setStyle(style);

			Slider depthSlider = new Slider();
			depthSlider.setPadding(new Insets(10, 10, 10, 10));
			depthSlider.setMin(30);
			depthSlider.setMax(130);
			depthSlider.setValue(30);
			depthSlider.setShowTickLabels(true);
			depthSlider.setShowTickMarks(true);
			depthSlider.setBlockIncrement(10);
			depthSlider.setMajorTickUnit(10);
			depthSlider.setMinorTickCount(10);
			depthSlider.setSnapToTicks(true);

			// Adding Listener to value property.
			depthSlider.valueProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

					// to save to main later
					savePlanDepth = newValue.intValue();

					lblDepthPlanner.setText(("Plan Depth \n30Ft " + newValue.intValue()) + "ft"); // casting to integer
				}
			});
			
			// Button save to main
			HBox hbForBtns = new HBox();
			hbForBtns.setAlignment(Pos.CENTER);

			Button btnSaveToMain = new Button("Add Plan To Log");
			btnSaveToMain.setStyle(style4);
			btnSaveToMain.setEffect(ds);

			Button btnReturnToMain = new Button("Return To Log");
			btnReturnToMain.setStyle(style4);
			btnReturnToMain.setEffect(ds);
			
			hbForBtns.getChildren().addAll(btnSaveToMain, btnReturnToMain);
			hbForBtns.setSpacing(60);

			// hBox for label new pressure group
			HBox hbNewPG = new HBox();
			hbNewPG.setSpacing(45);

			Label lblNewPressureGroup = new Label("New Pressure Group");
			lblNewPressureGroup.setStyle(style);

			hbNewPG.getChildren().addAll(lblNewPressureGroup, lblNewPG);

			HBox hbMaxBottomTime = new HBox();
			hbMaxBottomTime.setSpacing(65);

			Label lblMaxBottomTime = new Label("Max Bottom Time");
			lblMaxBottomTime.setStyle(style);

			Label lblMaxBT = new Label("N/A");
			lblMaxBT.setStyle("-fx-text-fill: orange;-fx-font-size: 16px;-fx-font-weight: bold");

			hbMaxBottomTime.getChildren().addAll(lblMaxBottomTime, lblMaxBT);
			vb.getChildren().addAll(hbPressureGroup, hbRN2, hbABT, hbRN, hbBottomTime);
			leftVbox.getChildren().addAll(vb, panePlanner, hbHours, hbMin);
			hbSlider.getChildren().addAll(lblDepthPlanner, depthSlider);

			// Adding Listener to value property for slider plan depth .
			depthSlider.valueProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					lblDepthPlanner.setText(("Plan Depth \n" + newValue.intValue() + "Ft")); // casting to integer

					// set label total bottom time
					// set label max plan depth
					lblMaxBT.setText("" + calculateMaxDiveTime(lblNewPG.getText().toCharArray()[0], newValue.intValue())
							+ " Min");

					// set label residual nitrogen time
					lblRn.setText("" + Integer.valueOf(rNt));

					// set total Bottom Time while slider moves
					lblBT.setText(String
							.valueOf((Integer.parseInt(lblRn.getText()) + Integer.parseInt(tfBottomTime.getText()))));

				}
			});

			rightVbox.getChildren().addAll(hbHours, hbMin, hbNewPG, hbSlider, hbMaxBottomTime);
			mainHbox.getChildren().addAll(leftVbox, rightVbox);

			// pane for planner screen
			BorderPane plannerPane = new BorderPane();
			plannerPane.setPadding(inSet);
			plannerPane.setBottom(hbForBtns);
			plannerPane.setRight(rightVbox);
			plannerPane.setLeft(mainHbox);
			plannerPane.setTop(vbLabel); // label Plan this Dive

			// button save
			btnSaveToMain.setOnAction(ee -> {

				tfPressureGroup1.setText(lblNewPG.getText());

				// Setting planned depth to main
				spinnerDepth.getValueFactory().setValue(Integer.valueOf((int) depthSlider.getValue()));

				// Setting planned
				stage.close();

			});
			// button return to main
			btnReturnToMain.setOnAction(eee -> {
				
				//setting temp values to main saved from planner 
				//used for return to main button (W/O saving )
				lblRn.setText(tempLblRn);
				lblABT.setText(templblAbt);
				lblBT.setText(templblBottomTime);
				lblPG.setText(tempPressureGroup);
				tfSurfaceInterval.setText(tempSI);
				
				stage.close();
			});

			Scene scene3 = new Scene(plannerPane, 575, 360);
			stage.setScene(scene3);
			stage.setTitle("Dive Plan");
			stage.show();

			// closing the primary stage
			primaryStage.setIconified(true);
			// centering the main stage in the screen
			Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
			primaryStage.setX((primScreenBounds.getWidth() - primaryStage.getWidth()) / 2);
			primaryStage.setY((primScreenBounds.getHeight() - primaryStage.getHeight()) / 4);

			// moving the dive profile to the plan pane on close //
			stage.setOnHiding(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent event) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {

							vbDisplay.getChildren().addAll(hbRN, hbABT, hbBottomTime, hbPressureGroup);

							// reopening the primaryStage
							primaryStage.setIconified(false);

						} // end run
					}); // end run later
				}
			}); // end set on hiding

		}); // end btnPlanner set on action

		// adding lines and text fields to the dive pane
		divePane.getChildren().addAll(line1, line2, line3, line4, line5, tfSurfaceInterval, tfPressureGroup1,
				tfPressureGroup2, tfDepth, tfBottomTime, lblSurfaceInterval, lblPressureGroup1, lblPressureGroup2,
				lblDepthMini, lblBottomTimeMini, lblBottomTimeTotalMini, tfBottomTimeTotal);

		// Adding tank sliders and labels to the hBox
		hBoxTankPsi.getChildren().addAll(lblTankPsiStart, sliderTankPsiStart, lblTankPsiEnd, sliderTankPsiEnd);

		// Adding temperature sliders and labels to the hBox that contains the
		// temperatures
		hBoxTemp.getChildren().addAll(lblAirTemp, sliderAir, lblWaterTemp, sliderWater);

		// Right side // hbHours, hbMin removed
		infoPaneRight.getChildren().addAll(hbCheckBoxes, vbDisplay, divePane, hbDepth, hbBottomTm);

		// Left side
		infoPaneLeft.getChildren().addAll(hbNameDiveNo, hBoxDate, hBoxLocation, hBoxTemp, hBoxTankPsi, hbWeight,
				lblComents, taComments);
		
		pane.setRight(infoPaneRight);
		pane.setLeft(infoPaneLeft);

		// creating a scene
		Scene scene2 = new Scene(pane, 615, 750);
		primaryStage.setScene(scene2);
		primaryStage.setTitle("Dive Log");
		primaryStage.show();

		
		
		
		// centering the main stage in the screen
		Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
		primaryStage.setX((primScreenBounds.getWidth() - primaryStage.getWidth()) / 2);
		primaryStage.setY((primScreenBounds.getHeight() - primaryStage.getHeight()) / 4);

		// when the main stage closes the file is saved to file
		primaryStage.setOnHiding(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {

						// writing log to file
						String fileName = name + "Data" + ".dat";

						try {

							ObjectOutputStream dataOut = new ObjectOutputStream(new FileOutputStream(fileName));

							dataOut.writeObject(user);
							dataOut.close();
							
						} catch (FileNotFoundException e1) {
							// e1.printStackTrace();
						} catch (IOException e1) {
							e1.printStackTrace();
						}

					} // end run
				}); // end run later
			}
		}); // end set on hiding
	}// end main log method

	// Create a saved log pop up only for feedback
	public void createPopup() {
		Stage popup = new Stage();
		
		popup.initModality(Modality.APPLICATION_MODAL); // freezes the main stage until the pop up window is closed
		popup.setTitle("LOG SAVED");
		DropShadow ds2 = new DropShadow();
		ds2.setOffsetY(4.0f);
		ds2.setColor(Color.color(0.4f, 0.4f, 0.4f));

		Label lblPopup = new Label("Log Saved");
		lblPopup.setStyle("-fx-text-fill: blue;-fx-font-size: 16px;-fx-font-weight: bold");
		lblPopup.setEffect(ds2);

		Button btnOK = new Button("OK");
		btnOK.setOnAction(e -> popup.close()); // close on button press 
		btnOK.setStyle("-fx-text-fill: steelblue;-fx-font-size: 16px;-fx-font-weight: bold");
		btnOK.setEffect(ds2);
		
		VBox layout = new VBox(10);
		layout.setStyle("-fx-border-color: steelblue;-fx-border-width: 15; -fx-border-radius: 20;-fx-border-style:solid;"
				+ "-fx-border-style: solid centered;");
		
		layout.getChildren().addAll(lblPopup, btnOK);
		layout.setAlignment(Pos.CENTER);

		Scene popupScene = new Scene(layout, 300, 100);
		popup.setScene(popupScene);
		popup.showAndWait();
	}

	// This method checks for max bottom time and will not allow the planner to plan
	// to deep of dives
	// or to long of dives
	public char checkBottomTimeMax(int depth, int min) {
		char max = ' ';
		if (depth <= 35 && min > 205) {
			max = '!';
		}

		if (depth >= 36 && depth <= 40 && min > 140) {
			max = '!';
		}
		if (depth >= 41 && depth <= 50 && min > 80) {
			max = '!';
		}
		if (depth >= 51 && depth <= 60 && min > 55) {
			max = '!';
		}
		if (depth >= 61 && depth <= 70 && min > 40) {
			max = '!';
		}
		if (depth >= 71 && depth <= 80 && min > 30) {
			max = '!';
		}
		if (depth >= 81 && depth <= 90 && min > 25) {
			max = '!';
		}
		if (depth >= 91 && depth <= 100 && min > 20) {
			return max;
		}
		if (depth >= 101 && depth <= 110 && min > 16) {
			max = '!';
		}
		if (depth >= 111 && depth <= 120 && min > 13) {
			max = '!';
		}
		if (depth >= 121 && depth <= 130 && min > 10) {
			max = '!';
		}
		if (depth >= 131 && depth <= 140 && min > 8) {
			max = '!';
		}
		return max;
	}

	// This method does two things
	// 1 Method calculates max dive time given a pressure group
	// 2 Method sets rNt residual nitrogen time from given pressure group
	public int calculateMaxDiveTime(char pg, int depth) {

		if (pg == 'A') {
			if (depth <= 35) {
				rNt = 10;
				return 195;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 9;
				return 131;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 7;
				return 73;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 6;
				return 49;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 5;
				return 35;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 4;
				return 26;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 4;
				return 21;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 3;
				return 17;
			}
			if (depth >= 101 && depth <= 110) {
				rNt = 3;
				return 13;
			}
			if (depth >= 111 && depth <= 120) {
				rNt = 3;
				return 10;
			}
			if (depth >= 121 && depth <= 130) {
				rNt = 3;
				return 7;
			}
		}
		if (pg == 'B') {
			if (depth <= 35) {
				rNt = 19;
				return 186;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 16;
				return 124;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 13;
				return 67;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 11;
				return 44;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 9;
				return 31;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 8;
				return 22;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 7;
				return 18;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 6;
				return 14;
			}
			if (depth >= 101 && depth <= 110) {
				rNt = 6;
				return 10;
			}
			if (depth >= 111 && depth <= 120) {
				rNt = 5;
				return 8;
			}
			if (depth >= 121 && depth <= 130) {
				rNt = 5;
				return 5;
			}
		}

		if (pg == 'C') {
			if (depth <= 35) {
				rNt = 25;
				return 180;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 22;
				return 118;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 17;
				return 63;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 14;
				return 41;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 12;
				return 28;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 10;
				return 20;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 9;
				return 16;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 8;
				return 12;
			}
			if (depth >= 101 && depth <= 110) {
				rNt = 7;
				return 9;
			}
			if (depth >= 111 && depth <= 120) {
				rNt = 6;
				return 7;
			}
			if (depth >= 121 && depth <= 130) {
				rNt = 6;
				return 4;
			}
		}

		if (pg == 'D') {
			if (depth <= 35) {
				rNt = 29;
				return 176;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 25;
				return 115;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 19;
				return 61;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 16;
				return 39;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 13;
				return 27;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 11;
				return 19;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 10;
				return 15;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 9;
				return 11;
			}
			if (depth >= 101 && depth <= 110) {
				rNt = 8;
				return 8;
			}
			if (depth >= 111 && depth <= 120) {
				rNt = 7;
				return 6;
			}
			if (depth >= 121 && depth <= 130) {
				rNt = 7;
				return 3;
			}
		}

		if (pg == 'E') {
			if (depth <= 35) {
				rNt = 32;
				return 173;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 27;
				return 113;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 21;
				return 59;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 17;
				return 38;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 15;
				return 25;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 13;
				return 17;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 11;
				return 14;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 10;
				return 10;
			}
			if (depth >= 101 && depth <= 110) {
				rNt = 9;
				return 7;
			}
			if (depth >= 111 && depth <= 120) {
				rNt = 8;
				return 5;
			}
		}

		if (pg == 'F') {
			if (depth <= 35) {
				rNt = 36;
				return 169;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 31;
				return 109;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 24;
				return 56;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 19;
				return 36;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 16;
				return 24;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 14;
				return 16;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 12;
				return 13;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 11;
				return 9;
			}
			if (depth >= 101 && depth <= 110) {
				rNt = 10;
				return 6;
			}
			if (depth >= 111 && depth <= 120) {
				rNt = 9;
				return 4;
			}
		}

		if (pg == 'G') {
			if (depth <= 35) {
				rNt = 40;
				return 165;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 34;
				return 106;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 26;
				return 54;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 21;
				return 34;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 18;
				return 22;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 15;
				return 15;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 13;
				return 12;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 12;
				return 8;
			}
			if (depth >= 101 && depth <= 110) {
				rNt = 11;
				return 5;
			}
			if (depth >= 111 && depth <= 120) {
				rNt = 10;
				return 3;
			}
		}

		if (pg == 'H') {
			if (depth <= 35) {
				rNt = 44;
				return 161;
			}
			if (depth >= 31 && depth <= 40) {
				rNt = 36;
				return 103;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 28;
				return 52;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 23;
				return 32;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 19;
				return 21;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 17;
				return 13;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 15;
				return 10;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 13;
				return 17;
			}
			if (depth >= 101 && depth <= 110) {
				rNt = 12;
				return 4;
			}
			if (depth >= 111 && depth <= 120) {
				rNt = 11;
				return 2;
			}
		}

		if (pg == 'I') {
			if (depth <= 35) {
				rNt = 48;
				return 157;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 40;
				return 100;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 31;
				return 49;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 25;
				return 30;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 21;
				return 19;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 18;
				return 12;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 16;
				return 9;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 14;
				return 6;
			}
			if (depth >= 101 && depth <= 110) {
				rNt = 13;
				return 3;
			}
		}
		if (pg == 'J') {
			if (depth <= 35) {
				rNt = 52;
				return 153;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 44;
				return 96;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 33;
				return 47;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 27;
				return 28;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 22;
				return 18;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 19;
				return 11;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 17;
				return 8;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 15;
				return 5;
			}
			if (depth >= 101 && depth <= 110) {
				rNt = 14;
				return 2;
			}
		}

		if (pg == 'K') {
			if (depth <= 35) {
				rNt = 57;
				return 148;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 48;
				return 92;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 36;
				return 44;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 29;
				return 26;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 24;
				return 16;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 21;
				return 9;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 18;
				return 7;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 16;
				return 4;
			}
			if (depth >= 101 && depth <= 110) {
				rNt = 14;
				return 2;
			}
		}

		if (pg == 'L') {
			if (depth <= 35) {
				rNt = 62;
				return 143;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 51;
				return 89;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 38;
				return 42;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 31;
				return 24;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 26;
				return 14;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 22;
				return 8;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 19;
				return 6;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 17;
				return 3;
			}
		}

		if (pg == 'M') {
			if (depth <= 35) {
				rNt = 67;
				return 138;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 55;
				return 85;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 41;
				return 39;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 33;
				return 22;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 27;
				return 13;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 23;
				return 7;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 21;
				return 4;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 18;
				return 2;
			}
		}

		if (pg == 'N') {
			if (depth <= 35) {
				rNt = 73;
				return 132;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 60;
				return 80;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 44;
				return 36;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 35;
				return 20;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 29;
				return 11;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 25;
				return 5;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 22;
				return 3;
			}
			if (depth >= 91 && depth <= 100) {
				rNt = 19;
				return 10;
			}
		}

		if (pg == 'O') {
			if (depth <= 35) {
				rNt = 79;
				return 126;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 64;
				return 76;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 47;
				return 33;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 37;
				return 18;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 31;
				return 9;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 26;
				return 4;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 23;
				return 2;
			}
		}

		if (pg == 'P') {
			if (depth <= 35) {
				rNt = 85;
				return 120;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 69;
				return 71;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 50;
				return 30;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 39;
				return 16;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 33;
				return 7;
			}
			if (depth >= 71 && depth <= 80) {
				rNt = 28;
				return 7;
			}
			if (depth >= 81 && depth <= 90) {
				rNt = 24;
				return 2;
			}
		}

		if (pg == 'Q') {
			if (depth <= 35) {
				rNt = 92;
				return 113;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 74;
				return 66;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 53;
				return 27;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 42;
				return 13;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 34;
				return 6;
			}
		}

		if (pg == 'R') {
			if (depth <= 35) {
				rNt = 100;
				return 105;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 79;
				return 61;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 57;
				return 23;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 44;
				return 11;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 36;
				return 4;
			}
		}

		if (pg == 'S') {
			if (depth <= 35) {
				rNt = 108;
				return 97;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 85;
				return 55;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 60;
				return 20;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 47;
				return 8;
			}
			if (depth >= 61 && depth <= 70) {
				rNt = 38;
				return 2;
			}
		}

		if (pg == 'T') {
			if (depth <= 35) {
				rNt = 117;
				return 88;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 91;
				return 49;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 63;
				return 17;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 49;
				return 6;
			}
		}

		if (pg == 'U') {
			if (depth <= 35) {
				rNt = 127;
				return 78;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 97;
				return 43;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 67;
				return 13;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 52;
				return 3;
			}
		}

		if (pg == 'V') {
			if (depth <= 35) {
				rNt = 139;
				return 66;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 104;
				return 36;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 71;
				return 9;
			}
			if (depth >= 51 && depth <= 60) {
				rNt = 54;
				return 1;
			}
		}

		if (pg == 'W') {
			if (depth <= 35) {
				rNt = 152;
				return 53;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 111;
				return 29;
			}
			if (depth >= 41 && depth <= 50) {
				rNt = 75;
				return 5;
			}
		}

		if (pg == 'X') {
			if (depth <= 35) {
				rNt = 168;
				return 37;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 120;
				return 20;
			}
		}

		if (pg == 'Y') {
			if (depth <= 35) {
				rNt = 188;
				return 17;
			}
			if (depth >= 36 && depth <= 40) {
				rNt = 129;
				return 11;
			}
		}

		return 0;
	}

	static char calculateFirstDivePressureGroup(int depth, int time) {

		char firstDivePressureGroup = 'A';

		// setting small depths from 0 to 30 to calculate as 30
		if (depth < 35) {
			depth = 35;

		}
		// rounding depth up to next depth to calculate pressure group
		if (depth <= 40 && depth >= 36) {
			depth = 40;
		}

		if (depth <= 50 && depth >= 41) {
			depth = 50;
		}
		if (depth <= 60 && depth >= 51) {
			depth = 60;
		}
		if (depth <= 70 && depth >= 61) {
			depth = 70;
		}
		if (depth <= 80 && depth >= 71) {
			depth = 80;
		}
		if (depth <= 90 && depth >= 81) {
			depth = 90;
		}
		if (depth < 100 && depth >= 91) {
			depth = 100;
		}
		if (depth <= 110 && depth >= 101) {
			depth = 110;
		}
		if (depth <= 120 && depth >= 111) {
			depth = 120;
		}
		if (depth <= 130 && depth >= 121) {
			depth = 130;
		}
		if (depth <= 140 && depth >= 131) {
			depth = 140;
		}

		// theList is the list of times in the depth column the depth is passed in as
		// depth
		int[] theList = { 0 };

		// depth 30
		int[] list35 = { 10, 19, 25, 29, 32, 36, 40, 44, 48, 52, 57, 62, 67, 73, 79, 85, 92, 100, 108, 117, 127, 139,
				152, 168, 188, 205 };
		// depth40
		int[] list40 = { 9, 16, 22, 25, 27, 31, 34, 37, 40, 44, 48, 51, 55, 60, 64, 69, 74, 79, 85, 91, 97, 104, 111,
				120, 129, 140 };
		// depth 50
		int[] list50 = { 7, 13, 17, 19, 21, 24, 26, 28, 31, 33, 36, 39, 41, 44, 47, 50, 53, 57, 60, 63, 67, 71, 75,
				80 };
		// depth 60
		int[] list60 = { 6, 11, 14, 16, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35, 37, 39, 42, 44, 47, 49, 52, 54, 55 };
		// Depth70
		int[] list70 = { 5, 9, 12, 13, 15, 16, 18, 19, 21, 22, 24, 26, 27, 29, 31, 33, 35, 36, 38, 40 };
		// Depth 80
		int[] list80 = { 4, 8, 10, 11, 13, 14, 15, 17, 18, 19, 21, 22, 23, 25, 26, 28, 29, 30 };
		// Depth 90
		int[] list90 = { 4, 7, 9, 10, 11, 12, 13, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25 };
		// Depth 100
		int[] list100 = { 3, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
		// Depth 110
		int[] list110 = { 3, 6, 7, 8, 9, 10, 11, 13, 13, 14, 15, 16 };
		// Depth 120
		int[] list120 = { 3, 5, 6, 7, 8, 9, 10, 11, 12, 13 };
		// Depth 130
		int[] list130 = { 3, 5, 6, 78, 9, 10 };
		// Depth 140
		int[] list140 = { 4, 5, 6, 7, 8 };

		switch (depth) {
		case 35:
			theList = list35;
			break;
		case 40:
			theList = list40;
			break;
		case 50:
			theList = list50;
			break;
		case 60:
			theList = list60;
			break;
		case 70:
			theList = list70;
			break;
		case 80:
			theList = list80;
			break;
		case 90:
			theList = list90;
			break;
		case 100:
			theList = list100;
			break;
		case 110:
			theList = list110;
			break;
		case 120:
			theList = list120;
			break;
		case 130:
			theList = list130;
			break;
		case 140:
			theList = list140;
			break;

		}
		for (int i = 0; i < theList.length; i++) {
			if (theList[i] == time) {
				firstDivePressureGroup = (char) (i + 65);
				break;
			}
		}
		// looping the times in the list to match bottom time
		for (int i = 0; i < theList.length; i++) {

			if (theList[i] < time) {
				firstDivePressureGroup = (char) (i + 65 + 1); // the +1 is to round up
				// firstDivePressureGroup = (char) (i + 65);

			}

		}

		return firstDivePressureGroup;
	}

	// this method calculates a new pressure group given a starting pressure-
	// group and surface interval time
	public char calculateNewPressureGroup(char group, int hrs, int min) {

		char newPressureGroup = ' ';

		int totalMin = (hrs * 60) + min;
		if (group == 'A') {
			return 'A';
		}
		if (group == 'B') {
			if (totalMin <= 47) {
				return 'B';
			}
			if (totalMin >= 48) {
				return 'A';
			}
		}

		if (group == 'C') {
			if (totalMin <= 21) {
				return 'C';
			}
			if (totalMin >= 22 && totalMin <= 69) {
				return 'B';
			}
			if (totalMin >= 70) {
				return 'A';
			}
		}

		if (group == 'D') {
			if (totalMin <= 8) {
				return 'D';
			}
			if (totalMin >= 9 && totalMin <= 30) {
				return 'C';
			}
			if (totalMin >= 31 && totalMin <= 78) {
				return 'B';
			}
			if (totalMin >= 79) {
				return 'A';
			}
		}

		if (group == 'E') {
			if (totalMin <= 7) {
				return 'E';
			}
			if (totalMin >= 8 && totalMin <= 16) {
				return 'D';
			}
			if (totalMin >= 17 && totalMin <= 38) {
				return 'C';
			}
			if (totalMin >= 39 && totalMin <= 87) {
				return 'B';
			}
			if (totalMin <= 88) {
				return 'A';
			}
		}

		if (group == 'F') {
			if (totalMin <= 7) {
				return 'F';
			}
			if (totalMin >= 8 && totalMin <= 15) {
				return 'E';
			}
			if (totalMin >= 16 && totalMin <= 24) {
				return 'D';
			}
			if (totalMin >= 25 && totalMin <= 46) {
				return 'C';
			}
			if (totalMin >= 47 && totalMin >= 94) {
				return 'B';
			}
			if (totalMin >= 95) {
				return 'A';
			}
		}

		if (group == 'G') {
			if (totalMin <= 6) {
				return 'G';
			}
			if (totalMin >= 7 && totalMin <= 13) {
				return 'F';
			}
			if (totalMin >= 14 && totalMin <= 22) {
				return 'E';
			}
			if (totalMin >= 23 && totalMin <= 31) {
				return 'D';
			}
			if (totalMin >= 32 && totalMin <= 53) {
				return 'C';
			}
			if (totalMin >= 54 && totalMin <= 101) {
				return 'B';
			}
			if (totalMin >= 102) {
				return 'A';
			}
		}

		if (group == 'H') {
			if (totalMin <= 5) {
				return 'H';
			}
			if (totalMin >= 6 && totalMin <= 12) {
				return 'G';
			}
			if (totalMin >= 13 && totalMin <= 20) {
				return 'F';
			}
			if (totalMin >= 21 && totalMin <= 28) {
				return 'E';
			}
			if (totalMin >= 29 && totalMin <= 37) {
				return 'D';
			}
			if (totalMin >= 38 && totalMin <= 59) {
				return 'C';
			}
			if (totalMin >= 60 && totalMin <= 107) {
				return 'B';
			}
			if (totalMin >= 108) {
				return 'A';
			}
		}

		if (group == 'I') {
			if (totalMin <= 5) {
				return 'I';
			}
			if (totalMin >= 6 && totalMin <= 11) {
				return 'H';
			}
			if (totalMin >= 12 && totalMin <= 18) {
				return 'G';
			}
			if (totalMin >= 19 && totalMin <= 26) {
				return 'F';
			}
			if (totalMin >= 27 && totalMin <= 34) {
				return 'E';
			}
			if (totalMin >= 35 && totalMin <= 43) {
				return 'D';
			}
			if (totalMin >= 44 & totalMin <= 65) {
				return 'C';
			}
			if (totalMin >= 66 && totalMin <= 113) {
				return 'B';
			}
			if (totalMin >= 114) {
				return 'A';
			}
		}

		if (group == 'J') {
			if (totalMin <= 5) {
				return 'J';
			}
			if (totalMin >= 6 && totalMin <= 11) {
				return 'I';
			}
			if (totalMin >= 12 && totalMin <= 17) {
				return 'H';
			}
			if (totalMin >= 18 && totalMin <= 24) {
				return 'G';
			}
			if (totalMin >= 15 && totalMin <= 31) {
				return 'F';
			}
			if (totalMin >= 32 && totalMin <= 40) {
				return 'E';
			}
			if (totalMin >= 41 && totalMin <= 49) {
				return 'D';
			}
			if (totalMin >= 50 && totalMin <= 71) {
				return 'C';
			}
			if (totalMin >= 72 && totalMin <= 119) {
				return 'B';
			}
			if (totalMin >= 120) {
				return 'A';
			}
		}

		if (group == 'K') {

			if (totalMin <= 4) {
				return 'K';
			}
			if (totalMin >= 5 && totalMin <= 10) {
				return 'J';
			}
			if (totalMin >= 11 && totalMin <= 16) {
				return 'I';
			}
			if (totalMin >= 17 && totalMin <= 22) {
				return 'H';
			}
			if (totalMin >= 23 && totalMin <= 29) {
				return 'G';
			}
			if (totalMin >= 30 && totalMin <= 37) {
				return 'F';
			}
			if (totalMin >= 38 && totalMin <= 45) {
				return 'E';
			}
			if (totalMin >= 46 && totalMin <= 54) {
				return 'D';
			}
			if (totalMin >= 55 && totalMin <= 76) {
				return 'C';
			}
			if (totalMin >= 77 && totalMin <= 124) {
				return 'B';
			}
			if (totalMin <= 125) {
				return 'A';
			}
		}

		if (group == 'L') {
			if (totalMin <= 4) {
				return 'L';
			}
			if (totalMin >= 5 && totalMin <= 9) {
				return 'K';
			}
			if (totalMin >= 10 && totalMin <= 15) {
				return 'J';
			}
			if (totalMin >= 16 && totalMin <= 21) {
				return 'I';
			}
			if (totalMin >= 22 && totalMin <= 27) {
				return 'H';
			}
			if (totalMin >= 28 && totalMin <= 34) {
				return 'G';
			}
			if (totalMin >= 35 && totalMin <= 42) {
				return 'F';
			}
			if (totalMin >= 43 && totalMin <= 50) {
				return 'E';
			}
			if (totalMin >= 51 && totalMin <= 59) {
				return 'D';
			}
			if (totalMin >= 60 && totalMin <= 81) {
				return 'C';
			}
			if (totalMin >= 82 && totalMin <= 129) {
				return 'B';
			}
			if (totalMin >= 130) {
				return 'A';
			}
		}

		if (group == 'M') {
			if (totalMin <= 4) {
				return 'M';
			}
			if (totalMin >= 5 && totalMin <= 9) {
				return 'L';
			}
			if (totalMin >= 10 && totalMin <= 14) {
				return 'K';
			}
			if (totalMin >= 15 && totalMin <= 19) {
				return 'J';
			}
			if (totalMin >= 20 && totalMin <= 25) {
				return 'I';
			}
			if (totalMin >= 26 && totalMin <= 32) {
				return 'H';
			}
			if (totalMin >= 33 && totalMin <= 39) {
				return 'G';
			}
			if (totalMin >= 40 && totalMin <= 46) {
				return 'F';
			}
			if (totalMin >= 47 && totalMin <= 55) {
				return 'E';
			}
			if (totalMin >= 56 && totalMin <= 64) {
				return 'D';
			}
			if (totalMin >= 65 && totalMin <= 85) {
				return 'C';
			}
			if (totalMin >= 86 && totalMin <= 134) {
				return 'B';
			}
			if (totalMin >= 135) {
				return 'A';
			}
		}

		if (group == 'N') {
			if (totalMin <= 3) {
				return 'N';
			}
			if (totalMin >= 4 && totalMin <= 8) {
				return 'M';
			}
			if (totalMin >= 9 && totalMin <= 13) {
				return 'L';
			}
			if (totalMin >= 14 && totalMin <= 18) {
				return 'K';
			}
			if (totalMin >= 19 && totalMin <= 24) {
				return 'J';
			}
			if (totalMin >= 25 && totalMin <= 30) {
				return 'I';
			}
			if (totalMin >= 31 && totalMin <= 36) {
				return 'H';
			}
			if (totalMin >= 37 && totalMin <= 43) {
				return 'G';
			}
			if (totalMin >= 44 && totalMin <= 51) {
				return 'F';
			}
			if (totalMin >= 52 && totalMin <= 59) {
				return 'E';
			}
			if (totalMin >= 60 && totalMin <= 68) {
				return 'D';
			}
			if (totalMin >= 69 && totalMin <= 90) {
				return 'C';
			}
			if (totalMin >= 91 && totalMin <= 138) {
				return 'B';
			}
			if (totalMin >= 139) {
				return 'A';
			}
		}

		if (group == 'O') {
			if (totalMin <= 3) {
				return 'O';
			}
			if (totalMin >= 4 && totalMin <= 8) {
				return 'N';
			}
			if (totalMin >= 9 && totalMin <= 12) {
				return 'M';
			}
			if (totalMin >= 13 && totalMin <= 17) {
				return 'L';
			}
			if (totalMin >= 18 && totalMin <= 23) {
				return 'K';
			}
			if (totalMin >= 24 && totalMin <= 28) {
				return 'J';
			}
			if (totalMin >= 29 && totalMin <= 34) {
				return 'I';
			}
			if (totalMin >= 35 && totalMin <= 41) {
				return 'H';
			}
			if (totalMin >= 42 && totalMin <= 47) {
				return 'G';
			}
			if (totalMin >= 48 && totalMin <= 55) {
				return 'F';
			}
			if (totalMin >= 56 && totalMin <= 63) {
				return 'E';
			}
			if (totalMin >= 64 && totalMin <= 72) {
				return 'D';
			}
			if (totalMin >= 73 && totalMin <= 94) {
				return 'C';
			}
			if (totalMin >= 95 && totalMin <= 143) {
				return 'B';
			}
			if (totalMin >= 144) {
				return 'A';
			}
		}

		if (group == 'P') {
			if (totalMin <= 3) {
				return 'P';
			}
			if (totalMin >= 4 && totalMin <= 7) {
				return 'O';
			}
			if (totalMin >= 8 && totalMin <= 12) {
				return 'N';
			}
			if (totalMin >= 13 && totalMin <= 16) {
				return 'M';
			}
			if (totalMin >= 17 && totalMin <= 21) {
				return 'L';
			}
			if (totalMin >= 22 && totalMin <= 27) {
				return 'K';
			}
			if (totalMin >= 28 && totalMin <= 32) {
				return 'J';
			}
			if (totalMin >= 33 && totalMin <= 38) {
				return 'I';
			}
			if (totalMin >= 39 && totalMin <= 45) {
				return 'H';
			}
			if (totalMin >= 46 && totalMin <= 51) {
				return 'G';
			}
			if (totalMin >= 52 && totalMin <= 59) {
				return 'F';
			}
			if (totalMin >= 60 && totalMin <= 67) {
				return 'E';
			}
			if (totalMin >= 68 && totalMin <= 76) {
				return 'D';
			}
			if (totalMin >= 77 && totalMin <= 98) {
				return 'C';
			}
			if (totalMin >= 99 && totalMin <= 147) {
				return 'B';
			}
			if (totalMin >= 148) {
				return 'A';
			}
		}

		if (group == 'Q') {
			if (totalMin <= 3) {
				return 'Q';
			}
			if (totalMin >= 4 && totalMin <= 7) {
				return 'P';
			}
			if (totalMin >= 8 && totalMin <= 11) {
				return 'O';
			}
			if (totalMin >= 12 && totalMin <= 16) {
				return 'N';
			}
			if (totalMin >= 17 && totalMin <= 20) {
				return 'M';
			}
			if (totalMin >= 21 && totalMin <= 25) {
				return 'L';
			}
			if (totalMin >= 26 && totalMin <= 30) {
				return 'K';
			}
			if (totalMin >= 31 && totalMin <= 36) {
				return 'J';
			}
			if (totalMin >= 37 && totalMin <= 44) {
				return 'I';
			}
			if (totalMin >= 43 && totalMin <= 48) {
				return 'H';
			}
			if (totalMin >= 49 && totalMin <= 55) {
				return 'G';
			}
			if (totalMin >= 56 && totalMin <= 63) {
				return 'F';
			}
			if (totalMin >= 64 && totalMin <= 71) {
				return 'E';
			}
			if (totalMin >= 72 && totalMin <= 80) {
				return 'D';
			}
			if (totalMin >= 81 && totalMin <= 102) {
				return 'C';
			}
			if (totalMin >= 103 && totalMin <= 150) {
				return 'B';
			}
			if (totalMin >= 151) {
				return 'A';
			}
		}

		if (group == 'R') {
			if (totalMin >= 3) {
				return 'R';
			}
			if (totalMin >= 4 && totalMin <= 7) {
				return 'Q';
			}
			if (totalMin >= 8 && totalMin <= 11) {
				return 'P';
			}
			if (totalMin >= 12 && totalMin <= 15) {
				return 'O';
			}
			if (totalMin >= 16 && totalMin <= 19) {
				return 'N';
			}
			if (totalMin >= 20 && totalMin <= 24) {
				return 'M';
			}
			if (totalMin >= 25 && totalMin <= 29) {
				return 'L';
			}
			if (totalMin >= 30 && totalMin <= 34) {
				return 'K';
			}
			if (totalMin >= 35 && totalMin <= 40) {
				return 'J';
			}
			if (totalMin >= 41 && totalMin <= 46) {
				return 'I';
			}
			if (totalMin >= 47 && totalMin <= 52) {
				return 'H';
			}
			if (totalMin >= 53 && totalMin <= 59) {
				return 'G';
			}
			if (totalMin >= 60 && totalMin <= 67) {
				return 'F';
			}
			if (totalMin >= 68 && totalMin <= 75) {
				return 'E';
			}
			if (totalMin >= 76 && totalMin <= 84) {
				return 'D';
			}
			if (totalMin >= 85 && totalMin <= 106) {
				return 'C';
			}
			if (totalMin >= 107 && totalMin <= 154) {
				return 'B';
			}
			if (totalMin >= 155) {
				return 'A';
			}
		}

		if (group == 'S') {
			if (totalMin <= 3) {
				return 'S';
			}
			if (totalMin >= 4 && totalMin <= 6) {
				return 'R';
			}
			if (totalMin >= 7 && totalMin <= 10) {
				return 'Q';
			}
			if (totalMin >= 11 && totalMin <= 14) {
				return 'P';
			}
			if (totalMin >= 15 && totalMin <= 18) {
				return 'O';
			}
			if (totalMin >= 19 && totalMin <= 23) {
				return 'N';
			}
			if (totalMin >= 24 && totalMin <= 27) {
				return 'M';
			}
			if (totalMin >= 28 && totalMin <= 32) {
				return 'L';
			}
			if (totalMin >= 33 && totalMin <= 38) {
				return 'K';
			}
			if (totalMin >= 39 && totalMin <= 43) {
				return 'J';
			}
			if (totalMin >= 44 && totalMin <= 49) {
				return 'I';
			}
			if (totalMin >= 50 && totalMin <= 56) {
				return 'H';
			}
			if (totalMin >= 57 && totalMin <= 63) {
				return 'G';
			}
			if (totalMin >= 64 && totalMin <= 70) {
				return 'F';
			}
			if (totalMin >= 71 && totalMin <= 78) {
				return 'E';
			}
			if (totalMin >= 79 && totalMin <= 87) {
				return 'D';
			}
			if (totalMin >= 88 && totalMin <= 109) {
				return 'C';
			}
			if (totalMin >= 110 && totalMin <= 158) {
				return 'B';
			}
			if (totalMin >= 159) {
				return 'A';
			}
		}

		if (group == 'T') {
			if (totalMin <= 2) {
				return 'T';
			}
			if (totalMin >= 3 && totalMin <= 6) {
				return 'S';
			}
			if (totalMin >= 7 && totalMin <= 10) {
				return 'R';
			}
			if (totalMin >= 11 && totalMin <= 13) {
				return 'Q';
			}
			if (totalMin >= 14 && totalMin <= 17) {
				return 'P';
			}
			if (totalMin >= 18 && totalMin <= 22) {
				return 'O';
			}
			if (totalMin >= 23 && totalMin <= 26) {
				return 'N';
			}
			if (totalMin >= 27 && totalMin <= 31) {
				return 'M';
			}
			if (totalMin >= 32 && totalMin <= 36) {
				return 'L';
			}
			if (totalMin >= 37 && totalMin <= 41) {
				return 'K';
			}
			if (totalMin >= 42 && totalMin <= 47) {
				return 'J';
			}
			if (totalMin >= 48 && totalMin <= 53) {
				return 'I';
			}
			if (totalMin >= 54 && totalMin <= 59) {
				return 'H';
			}
			if (totalMin >= 60 && totalMin <= 66) {
				return 'G';
			}
			if (totalMin >= 67 && totalMin <= 73) {
				return 'F';
			}
			if (totalMin >= 74 && totalMin <= 82) {
				return 'E';
			}
			if (totalMin >= 83 && totalMin <= 91) {
				return 'D';
			}
			if (totalMin >= 92 && totalMin <= 113) {
				return 'C';
			}
			if (totalMin >= 114 && totalMin <= 161) {
				return 'B';
			}
			if (totalMin >= 162) {
				return 'A';
			}
		}

		if (group == 'U') {
			if (totalMin <= 2) {
				return 'U';
			}
			if (totalMin >= 3 && totalMin <= 6) {
				return 'T';
			}
			if (totalMin >= 7 && totalMin <= 9) {
				return 'S';
			}
			if (totalMin >= 10 && totalMin <= 13) {
				return 'R';
			}
			if (totalMin >= 14 && totalMin <= 17) {
				return 'Q';
			}
			if (totalMin >= 18 && totalMin <= 21) {
				return 'P';
			}
			if (totalMin >= 22 && totalMin <= 25) {
				return 'O';
			}
			if (totalMin >= 26 && totalMin <= 29) {
				return 'N';
			}
			if (totalMin >= 30 && totalMin <= 34) {
				return 'M';
			}
			if (totalMin >= 35 && totalMin <= 39) {
				return 'L';
			}
			if (totalMin >= 40 && totalMin <= 44) {
				return 'K';
			}
			if (totalMin >= 45 && totalMin <= 50) {
				return 'J';
			}
			if (totalMin >= 51 && totalMin <= 56) {
				return 'I';
			}
			if (totalMin >= 57 && totalMin <= 62) {
				return 'H';
			}
			if (totalMin >= 63 && totalMin <= 69) {
				return 'G';
			}
			if (totalMin >= 70 && totalMin <= 77) {
				return 'F';
			}
			if (totalMin >= 78 && totalMin <= 85) {
				return 'E';
			}
			if (totalMin >= 86 && totalMin <= 94) {
				return 'D';
			}
			if (totalMin >= 95 && totalMin <= 116) {
				return 'C';
			}
			if (totalMin >= 117 && totalMin <= 164) {
				return 'B';
			}
			if (totalMin >= 165) {
				return 'A';
			}
		}

		if (group == 'V') {
			if (totalMin <= 2) {
				return 'V';
			}
			if (totalMin >= 3 && totalMin <= 5) {
				return 'U';
			}
			if (totalMin >= 6 && totalMin <= 9) {
				return 'T';
			}
			if (totalMin >= 10 && totalMin <= 12) {
				return 'S';
			}
			if (totalMin >= 13 && totalMin <= 16) {
				return 'R';
			}
			if (totalMin >= 17 && totalMin <= 20) {
				return 'Q';
			}
			if (totalMin >= 21 && totalMin <= 24) {
				return 'P';
			}
			if (totalMin >= 25 && totalMin <= 28) {
				return 'O';
			}
			if (totalMin >= 29 && totalMin <= 33) {
				return 'N';
			}
			if (totalMin >= 34 && totalMin <= 37) {
				return 'M';
			}
			if (totalMin >= 38 && totalMin <= 42) {
				return 'L';
			}
			if (totalMin >= 43 && totalMin <= 47) {
				return 'K';
			}
			if (totalMin >= 48 && totalMin <= 53) {
				return 'J';
			}
			if (totalMin >= 54 && totalMin <= 59) {
				return 'I';
			}
			if (totalMin >= 60 && totalMin <= 65) {
				return 'H';
			}
			if (totalMin >= 66 && totalMin <= 72) {
				return 'G';
			}
			if (totalMin >= 73 && totalMin <= 80) {
				return 'F';
			}
			if (totalMin >= 81 && totalMin <= 88) {
				return 'E';
			}
			if (totalMin >= 89 && totalMin <= 97) {
				return 'D';
			}
			if (totalMin >= 98 && totalMin <= 119) {
				return 'C';
			}
			if (totalMin >= 120 && totalMin <= 167) {
				return 'B';
			}
			if (totalMin >= 168) {
				return 'A';
			}
		}

		if (group == 'W') {
			if (totalMin <= 2) {
				return 'W';
			}
			if (totalMin >= 3 && totalMin <= 5) {
				return 'V';
			}
			if (totalMin >= 6 && totalMin <= 8) {
				return 'U';
			}
			if (totalMin >= 9 && totalMin <= 12) {
				return 'T';
			}
			if (totalMin >= 13 && totalMin <= 15) {
				return 'S';
			}
			if (totalMin >= 16 && totalMin <= 19) {
				return 'R';
			}
			if (totalMin >= 20 && totalMin <= 23) {
				return 'Q';
			}
			if (totalMin >= 24 && totalMin <= 27) {
				return 'P';
			}
			if (totalMin >= 28 && totalMin <= 31) {
				return 'O';
			}
			if (totalMin >= 32 && totalMin <= 36) {
				return 'N';
			}
			if (totalMin >= 37 && totalMin <= 40) {
				return 'M';
			}
			if (totalMin >= 41 && totalMin <= 45) {
				return 'L';
			}
			if (totalMin >= 46 && totalMin <= 50) {
				return 'K';
			}
			if (totalMin >= 51 && totalMin <= 56) {
				return 'J';
			}
			if (totalMin >= 57 && totalMin <= 62) {
				return 'I';
			}
			if (totalMin >= 63 && totalMin <= 68) {
				return 'H';
			}
			if (totalMin >= 69 && totalMin <= 75) {
				return 'G';
			}
			if (totalMin >= 76 && totalMin <= 83) {
				return 'F';
			}
			if (totalMin >= 84 && totalMin <= 91) {
				return 'E';
			}
			if (totalMin >= 92 && totalMin <= 100) {
				return 'D';
			}
			if (totalMin >= 101 && totalMin <= 122) {
				return 'C';
			}
			if (totalMin >= 123 && totalMin <= 170) {
				return 'B';
			}
			if (totalMin >= 171) {
				return 'A';
			}
		}

		if (group == 'X') {
			if (totalMin <= 2) {
				return 'X';
			}
			if (totalMin >= 3 && totalMin <= 5) {
				return 'W';
			}
			if (totalMin >= 6 && totalMin <= 8) {
				return 'V';
			}
			if (totalMin >= 9 && totalMin <= 11) {
				return 'U';
			}
			if (totalMin >= 12 && totalMin <= 15) {
				return 'T';
			}
			if (totalMin >= 16 && totalMin <= 18) {
				return 'S';
			}
			if (totalMin >= 19 && totalMin <= 22) {
				return 'R';
			}
			if (totalMin >= 23 && totalMin <= 26) {
				return 'Q';
			}
			if (totalMin >= 27 && totalMin <= 30) {
				return 'P';
			}
			if (totalMin >= 31 && totalMin <= 34) {
				return 'O';
			}
			if (totalMin >= 35 && totalMin <= 39) {
				return 'N';
			}
			if (totalMin >= 40 && totalMin <= 43) {
				return 'M';
			}
			if (totalMin >= 44 && totalMin <= 48) {
				return 'L';
			}
			if (totalMin >= 49 && totalMin <= 53) {
				return 'K';
			}
			if (totalMin >= 54 && totalMin <= 59) {
				return 'J';
			}
			if (totalMin >= 60 && totalMin <= 65) {
				return 'I';
			}
			if (totalMin >= 66 && totalMin <= 71) {
				return 'H';
			}
			if (totalMin >= 72 & totalMin <= 78) {
				return 'G';
			}
			if (totalMin >= 79 && totalMin <= 86) {
				return 'F';
			}
			if (totalMin >= 87 && totalMin <= 94) {
				return 'E';
			}
			if (totalMin >= 95 && totalMin <= 103) {
				return 'D';
			}
			if (totalMin >= 104 && totalMin <= 125) {
				return 'C';
			}
			if (totalMin >= 126 && totalMin <= 173) {
				return 'B';
			}
			if (totalMin >= 174) {
				return 'A';
			}
		}

		if (group == 'Y') {
			if (totalMin <= 2) {
				return 'Y';
			}
			if (totalMin >= 3 && totalMin <= 5) {
				return 'X';
			}
			if (totalMin >= 6 && totalMin <= 8) {
				return 'W';
			}
			if (totalMin >= 9 && totalMin <= 11) {
				return 'V';
			}
			if (totalMin >= 12 && totalMin <= 14) {
				return 'U';
			}
			if (totalMin >= 15 && totalMin <= 18) {
				return 'T';
			}
			if (totalMin >= 19 && totalMin <= 21) {
				return 'S';
			}
			if (totalMin >= 22 && totalMin <= 25) {
				return 'R';
			}
			if (totalMin >= 26 && totalMin <= 29) {
				return 'Q';
			}
			if (totalMin >= 30 && totalMin <= 33) {
				return 'P';
			}
			if (totalMin >= 34 && totalMin <= 37) {
				return 'O';
			}
			if (totalMin >= 38 && totalMin <= 41) {
				return 'N';
			}
			if (totalMin >= 42 && totalMin <= 46) {
				return 'M';
			}
			if (totalMin >= 47 && totalMin <= 51) {
				return 'L';
			}
			if (totalMin >= 52 && totalMin <= 56) {
				return 'K';
			}
			if (totalMin >= 57 && totalMin <= 62) {
				return 'J';
			}
			if (totalMin >= 63 && totalMin <= 68) {
				return 'I';
			}
			if (totalMin >= 69 && totalMin <= 74) {
				return 'H';
			}
			if (totalMin >= 74 && totalMin <= 81) {
				return 'G';
			}
			if (totalMin >= 82 && totalMin <= 89) {
				return 'F';
			}
			if (totalMin >= 90 && totalMin <= 97) {
				return 'E';
			}
			if (totalMin >= 98 && totalMin <= 106) {
				return 'D';
			}
			if (totalMin >= 107 && totalMin <= 128) {
				return 'C';
			}
			if (totalMin >= 129 && totalMin <= 176) {
				return 'B';
			}
			if (totalMin <= 177) {
				return 'A';
			}
		}

		if (group == 'Z') {
			if (totalMin <= 2) {
				return 'Z';
			}
			if (totalMin >= 3 && totalMin <= 5) {
				return 'Y';
			}
			if (totalMin >= 6 && totalMin <= 8) {
				return 'X';
			}
			if (totalMin >= 9 && totalMin <= 11) {
				return 'W';
			}
			if (totalMin >= 12 && totalMin <= 14) {
				return 'V';
			}
			if (totalMin >= 15 && totalMin <= 17) {
				return 'U';
			}
			if (totalMin >= 18 && totalMin <= 20) {
				return 'T';
			}
			if (totalMin >= 21 && totalMin <= 24) {
				return 'S';
			}
			if (totalMin >= 25 && totalMin <= 28) {
				return 'R';
			}
			if (totalMin >= 29 && totalMin <= 31) {
				return 'Q';
			}
			if (totalMin >= 32 && totalMin <= 35) {
				return 'P';
			}
			if (totalMin >= 36 && totalMin <= 40) {
				return 'O';
			}
			if (totalMin >= 41 && totalMin <= 44) {
				return 'N';
			}
			if (totalMin >= 45 && totalMin <= 49) {
				return 'M';
			}
			if (totalMin >= 50 && totalMin <= 54) {
				return 'L';
			}
			if (totalMin >= 55 && totalMin <= 59) {
				return 'J';
			}
			if (totalMin >= 60 && totalMin <= 65) {
				return 'I';
			}
			if (totalMin >= 66 && totalMin <= 71) {
				return 'H';
			}
			if (totalMin >= 72 && totalMin <= 77) {
				return 'G';
			}
			if (totalMin >= 78 && totalMin <= 84) {
				return 'F';
			}
			if (totalMin >= 85 && totalMin <= 91) {
				return 'E';
			}
			if (totalMin >= 92 && totalMin <= 100) {
				return 'D';
			}
			if (totalMin >= 101 && totalMin <= 109) {
				return 'C';
			}
			if (totalMin >= 110 && totalMin <= 131) {
				return 'B';
			}
			if (totalMin >= 132) {
				return 'A';
			}
		}

		return newPressureGroup;
	}

}// end DiveLog class-----------------------------------------------
