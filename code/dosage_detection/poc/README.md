# Dia-Smart: Dosage Detection Node

This module handles the computer vision and AI processing for the Dia-Smart proof-of-concept. It captures a video feed of a Humulin 30/70 insulin pen, detects the pre-injection pause, and extracts the exact dose using temporal reasoning.

## Technology Stack
* **Language:** Python 3.x
* **Computer Vision:** OpenCV (`cv2`) for local webcam capture and hardware control.
* **AI Processing:** Google Gemini 2.5 Flash (`google-genai`) for multimodal video analysis and Chain of Thought reasoning.

## Setup Instructions
1. Navigate to this directory in your terminal.
2. Install the required dependencies:
   ```bash
   pip install -r requirements.txt