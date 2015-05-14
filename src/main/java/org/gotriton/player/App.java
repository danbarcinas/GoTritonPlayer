package org.gotriton.player;
	
import java.io.File;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/*
 * GoTriton Player
 * 
 * @author Daniel Barcinas
 * @author Ariel Yamaguchi
 * @version 0.0.1
 */
public class App extends Application {
	
	private Media media;
	private MediaPlayer mediaPlayer;
	private MediaView mediaView;
	
	private Duration duration;
	
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		
		// Create root window
		BorderPane root = new BorderPane();
		
		// Build player controls
		HBox controlPane = new HBox();
		controlPane.setAlignment(Pos.CENTER);
		controlPane.setSpacing(5);
		controlPane.setPadding(new Insets(6, 10, 6, 10));
		controlPane.setStyle("-fx-background-color: #eee");
		
		// Disable the controls until user prompts media file
		controlPane.setDisable(true);

		Image playIcon = new Image(getClass().getResourceAsStream("/images/play.png"));
		Image pauseIcon = new Image(getClass().getResourceAsStream("/images/pause.png"));
		Image fullScreenIcon = new Image(getClass().getResourceAsStream("/images/fullscreen.png"));
		
		Button playPauseButton = new Button(null, new ImageView(playIcon));
		playPauseButton.setStyle("-fx-background-color: transparent");

		playPauseButton.setOnAction(event -> {
			Status status = mediaPlayer.getStatus();
			if (status == Status.UNKNOWN || status == Status.HALTED) {
				return; // Do nothing at these states
			}
			
			if (status == Status.PAUSED ||
				status == Status.READY  ||
				status == Status.STOPPED) {
				mediaPlayer.play();
			} else {
				mediaPlayer.pause();
			}
		});
		
		// @TODO Toggle full screen mode
		Button fullScreenButton = new Button(null, new ImageView(fullScreenIcon));
		fullScreenButton.setStyle("-fx-background-color: transparent");
		fullScreenButton.setOnAction(event -> {
			stage.setFullScreen(true);
		});
		
		Slider timeSlider = new Slider();
		HBox.setHgrow(timeSlider, Priority.ALWAYS);
		timeSlider.setMinWidth(50);
        timeSlider.setMaxWidth(Double.MAX_VALUE);
		timeSlider.valueProperty().addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable observable) {
				if (timeSlider.isValueChanging()) {
                    // Multiply duration by percentage calculated by slider position
                    mediaPlayer.seek(duration.multiply(timeSlider.getValue() / 100.0));
                }
			}
		});
		
		Label timeLabel = new Label("00:00/00:00");
		timeLabel.setPrefWidth(150);
		timeLabel.setMinWidth(50);
		
		Image volumeIcon = new Image(getClass().getResourceAsStream("/images/volume.png"));
		Label volumeLabel = new Label(null, new ImageView(volumeIcon));
		
		Slider volumeSlider = new Slider();
		volumeSlider.setPrefWidth(90);
        volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
        volumeSlider.setMinWidth(30);
        volumeSlider.setValue(0.2 * 100);
        volumeSlider.valueProperty().addListener(new InvalidationListener() {
        	@Override
            public void invalidated(Observable observable) {
                if (volumeSlider.isValueChanging()) {
                    mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);
                }
            }
        });
		
		controlPane.getChildren().addAll(playPauseButton,
				fullScreenButton, timeSlider, timeLabel, volumeLabel, volumeSlider);
		
		// Build menu
		VBox menuPane = new VBox();
		
		MenuBar menuBar = new MenuBar();
		
		Menu fileMenu = new Menu("_File");
		
		MenuItem openMenuItem = new MenuItem("Open media file...");
		openMenuItem.setOnAction(event -> {
			Stage promptWindow = new Stage();
			
			VBox promptWindowPane = new VBox();
			promptWindowPane.setPadding(new Insets(10));
			
			Label label = new Label("Enter URL or browse for a local file:");
			VBox.setMargin(label, new Insets(0, 0, 8, 0));
			
			TextField text = new TextField();
			VBox.setMargin(text, new Insets(0, 0, 8, 0));
			
			HBox buttons = new HBox();
			buttons.setSpacing(5);
			buttons.setAlignment(Pos.CENTER);
			
			Button browseButton = new Button("Browse...");
			browseButton.setOnAction(e -> {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
				fileChooser.setTitle("Choose a media file");
				fileChooser.getExtensionFilters().add(
						new FileChooser.ExtensionFilter("Video, Audio", "*.mp4", "*.mp3", "*.wav", "*.m4a"));
				
				File selectedFile = fileChooser.showOpenDialog(stage);
				
				if (selectedFile != null) {
					String mediaFileUri = selectedFile.toURI().toString().trim();
					text.setText(mediaFileUri);
				}
			});
			
			Button openButton = new Button("Open");
			openButton.setOnAction(e -> {
				// @TODO There's got to be a better way!
				if (text.getText().length() > 10) {
					
					// Media
					VBox mediaPane = new VBox();
					mediaPane.setAlignment(Pos.CENTER);
					mediaPane.setStyle("-fx-background-color: #000");
					
					media = new Media(text.getText());
					
					mediaPlayer = new MediaPlayer(media);
					mediaPlayer.setAutoPlay(true);
					mediaPlayer.setVolume(0.2);
					
					mediaPlayer.currentTimeProperty().addListener(new InvalidationListener() {
						@Override
			            public void invalidated(Observable observable) {
							updateTime(timeLabel, timeSlider, volumeSlider);
			            }
			        });
					
					mediaPlayer.setOnPaused(new Runnable() {
						@Override
						public void run() {
							playPauseButton.setGraphic(new ImageView(playIcon));
						}
					});
					mediaPlayer.setOnPlaying(new Runnable() {
						@Override
						public void run() {
							playPauseButton.setGraphic(new ImageView(pauseIcon));
						}
					});
					mediaPlayer.setOnReady(new Runnable() {
						@Override
						public void run() {
							duration = mediaPlayer.getMedia().getDuration();
							updateTime(timeLabel, timeSlider, volumeSlider);
						}
					});
					mediaPlayer.setOnEndOfMedia(new Runnable() {
						@Override
						public void run() {
							mediaPlayer.stop();
							playPauseButton.setGraphic(new ImageView(playIcon));
							System.out.println("DONE");
						}
					});
					
					mediaView = new MediaView(mediaPlayer);
					mediaView.fitWidthProperty().bind(root.widthProperty());
					// @TODO Well, shit!
					mediaView.fitHeightProperty().bind(root.heightProperty().subtract(70));
					
					mediaPane.getChildren().add(mediaView);
					
					// Set the mediaPane to center position of root node
					root.setCenter(mediaPane);
					
					// All clear! Close prompt window and enable controls
					promptWindow.close();
					controlPane.setDisable(false);
				}
			});
			
			buttons.getChildren().addAll(browseButton, openButton);
			
			// Add all nodes to promptWindowPane
			promptWindowPane.getChildren().addAll(label, text, buttons);
			
			promptWindow.setScene(new Scene(promptWindowPane, 500, 100));
			promptWindow.setTitle("Open media file");
			promptWindow.setResizable(false);
			promptWindow.initModality(Modality.APPLICATION_MODAL);
			promptWindow.show();
		});
		
		MenuItem exitMenuItem = new MenuItem("Exit");
		exitMenuItem.setOnAction(event -> Platform.exit());
		
		fileMenu.getItems().addAll(openMenuItem, new SeparatorMenuItem(), exitMenuItem);
		
		menuBar.getMenus().add(fileMenu);
		menuPane.getChildren().add(menuBar);
		
		// Setup position for nodes to root
		root.setTop(menuPane);
		root.setBottom(controlPane);
		
		// Set scene and show stage
		Scene scene = new Scene(root, 800, 600);
		scene.getStylesheets().add(getClass().getResource("/ui-style.css").toExternalForm());
		
		stage.setScene(scene);
		stage.setTitle("GoTriton Player");
		stage.show();
	}
	
	protected void updateTime(Label timeLabel, Slider timeSlider, Slider volumeSlider) {
        if (timeLabel != null && timeSlider != null && volumeSlider != null) {
            Platform.runLater(new Runnable() {
                public void run() {
                    Duration currentTime = mediaPlayer.getCurrentTime();
                	timeLabel.setText(formatTime(currentTime, duration));
                    timeSlider.setDisable(duration.isUnknown());
                    if (!timeSlider.isDisabled()
                            && duration.greaterThan(Duration.ZERO)
                            && !timeSlider.isValueChanging()) {
                    	
                    	// @TODO dividing a duration is deprecated, use double instead
                        timeSlider.setValue(currentTime.divide(duration).toMillis() * 100.0);
                    }

                    if (!volumeSlider.isValueChanging()) {
                        volumeSlider.setValue((int) Math.round(mediaPlayer.getVolume() * 100));
                    }
                }
            });
        }
    }
	
	protected static String formatTime(Duration elapsed, Duration duration) {
        int intElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        if (elapsedHours > 0) {
            intElapsed -= elapsedHours * 60 * 60;
        }
        
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) Math.floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration - durationHours * 60 * 60
                    - durationMinutes * 60;
            if (durationHours > 0) {
                return String.format("%d:%02d:%02d/%d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds,
                        durationHours, durationMinutes, durationSeconds);
            } else {
                return String.format("%02d:%02d/%02d:%02d",
                        elapsedMinutes, elapsedSeconds, durationMinutes,
                        durationSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
            }
        }
    }
	
	
}
