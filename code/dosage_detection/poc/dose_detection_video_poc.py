import cv2
import time
import os
from google import genai

# ==========================================
# 1. SETUP GOOGLE GEMINI API (NEW SDK)
# ==========================================
# ⚠️ REPLACE THIS WITH YOUR ACTUAL API KEY (Starts with AIzaSy...)
client = genai.Client(api_key="AIzaSyCbOx00s5yfut9w1VcmuKdMbNwnluTrNCY")

# ==========================================
# 2. VIDEO ANALYSIS FUNCTION
# ==========================================
def analyze_video(filepath):
    print(f"\nUploading video to Gemini API...")
    try:
        # Upload the video
        video_file = client.files.upload(file=filepath)
        
        # Wait for Google to process the file
        print("Waiting for Google to process the video data...")
        while video_file.state.name == "PROCESSING":
            print(".", end="", flush=True)
            time.sleep(1)
            video_file = client.files.get(name=video_file.name)
        print() 
        
        if video_file.state.name == "FAILED":
            print("Error: Video processing failed on Google's side.")
            return

        print("Analyzing the sequence for injection movement (Chain of Thought)...")
        
        # The Chain of Thought Prompt
        prompt = (
            "You are a medical verification AI analyzing a video of an insulin pen. "
            "The user will dial a dose, pause, and then push the top knob down to inject. "
            "I need to know the exact dose locked in DURING THE PAUSE, right before the push. "
            "\n\nFirst, describe the timeline of the video step-by-step: "
            "1. What happens during the dialing phase? "
            "2. When does the dialing stop, and what does the dose window look like during the pause? "
            "3. At what exact moment does the top knob get pushed down? "
            "\n\nCRITICAL DIAL RULES: Even numbers (10, 12) are printed. Odd numbers are a single line "
            "centered between two even numbers (e.g., a line between 12 and 14 is 13). "
            "\n\nAfter your step-by-step analysis, output the final locked-in dose on a new line formatted exactly like this: "
            "FINAL_DOSE_RESULT: [number]"
        )
        
        # Generate the response
        response = client.models.generate_content(
            model='gemini-2.5-flash',
            contents=[video_file, prompt]
        )
        
        # Print the AI's internal reasoning
        print("\n--- AI THOUGHT PROCESS ---")
        print(response.text.strip())
        print("--------------------------\n")
        
        # Extract the final dose
        if "FINAL_DOSE_RESULT:" in response.text:
            final_dose = response.text.split("FINAL_DOSE_RESULT:")[1].strip()
            # Clean up any extra characters just in case
            final_dose = ''.join(filter(str.isdigit, final_dose))
            
            print("=====================================")
            print(f"✅ DETECTED INJECTED DOSE: {final_dose} Units")
            print("=====================================\n")
        else:
            print("⚠️ Could not extract the final dose format from the AI's response.")
            
        print("Ready for next scan. Press SPACE to record again.")
        
        # Clean up the file from Google's servers
        client.files.delete(name=video_file.name)
        
    except Exception as e:
        # THIS is the block that was missing or misaligned!
        print(f"API Error: {e}")
# ==========================================
# 3. MAIN WEBCAM LOGIC
# ==========================================
def main():
    print("Starting Dia-Smart Video Node...")
    cap = cv2.VideoCapture(0, cv2.CAP_DSHOW) 
    
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = None
    is_recording = False
    video_filename = "temp_dose_video.mp4"

    print("Camera loaded. Press SPACEBAR to start/stop recording. Press 'q' to quit.")

    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                print("Failed to grab camera frame. Check connection.")
                break

            # --- UI AND RECORDING ---
            if is_recording:
                cv2.putText(frame, "RECORDING... (Press SPACE to Stop)", (10, 50), 
                            cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)
                if out is not None:
                    out.write(frame)
            else:
                cv2.putText(frame, "READY. (Press SPACE to Start)", (10, 50), 
                            cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)

            cv2.imshow("Dia-Smart Video PoC", frame)
            
            key = cv2.waitKey(1) & 0xFF
            
            if key == 32: 
                if not is_recording:
                    print("\n[🔴 RECORDING STARTED] Dial your dose and inject...")
                    height, width, _ = frame.shape
                    out = cv2.VideoWriter(video_filename, fourcc, 20.0, (width, height))
                    is_recording = True
                else:
                    print("\n[⏹ RECORDING STOPPED] Compiling video...")
                    is_recording = False
                    out.release() 
                    analyze_video(video_filename)
                    
            elif key == ord('q'):
                break
                
    except Exception as e:
        print(f"\nAn error occurred: {e}")
        
    finally:
        print("\nCleaning up hardware locks...")
        if cap.isOpened():
            cap.release()
        if out is not None:
            out.release()
        cv2.destroyAllWindows()
        if os.path.exists(video_filename):
            try:
                os.remove(video_filename)
            except:
                pass
        print("Camera released safely.")

if __name__ == "__main__":
    main()