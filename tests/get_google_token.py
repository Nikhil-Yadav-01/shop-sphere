import http.server
import socketserver
import urllib.parse
import urllib.request
import json
import webbrowser
import os
import sys

# You can pass these via environment variables or hardcode them here for testing
CLIENT_ID = os.environ.get('GOOGLE_CLIENT_ID', 'YOUR_GOOGLE_CLIENT_ID')
CLIENT_SECRET = os.environ.get('GOOGLE_CLIENT_SECRET', 'YOUR_GOOGLE_CLIENT_SECRET')
REDIRECT_URI = 'http://localhost:8080/callback'

if CLIENT_ID == 'YOUR_GOOGLE_CLIENT_ID':
    print("❌ ERROR: Please set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET")
    print("Run like this:")
    print("set GOOGLE_CLIENT_ID=xxx")
    print("set GOOGLE_CLIENT_SECRET=yyy")
    print("python get_google_token.py")
    sys.exit(1)

class OAuthHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        parsed_path = urllib.parse.urlparse(self.path)
        
        if parsed_path.path == '/':
            # Step 1: Redirect to Google Auth
            auth_url = (
                f"https://accounts.google.com/o/oauth2/v2/auth?"
                f"client_id={CLIENT_ID}&"
                f"redirect_uri={urllib.parse.quote(REDIRECT_URI)}&"
                f"response_type=code&"
                f"scope=openid%20email%20profile"
            )
            self.send_response(302)
            self.send_header('Location', auth_url)
            self.end_headers()
            
        elif parsed_path.path == '/callback':
            # Step 2: Handle callback and exchange code for tokens
            query_components = urllib.parse.parse_qs(parsed_path.query)
            
            if 'code' in query_components:
                code = query_components['code'][0]
                
                # Exchange code for token
                token_url = "https://oauth2.googleapis.com/token"
                data = urllib.parse.urlencode({
                    'code': code,
                    'client_id': CLIENT_ID,
                    'client_secret': CLIENT_SECRET,
                    'redirect_uri': REDIRECT_URI,
                    'grant_type': 'authorization_code'
                }).encode('utf-8')
                
                req = urllib.request.Request(token_url, data=data)
                try:
                    with urllib.request.urlopen(req) as response:
                        token_data = json.loads(response.read().decode('utf-8'))
                        id_token = token_data.get('id_token')
                        
                        self.send_response(200)
                        self.send_header('Content-type', 'text/html')
                        self.end_headers()
                        self.wfile.write(b"<h1>Success!</h1><p>Check your terminal for the ID token.</p>")
                        
                        print("\n" + "="*80)
                        print("✅ SUCCESS! Here is your Google ID Token:")
                        print("="*80)
                        print(id_token)
                        print("="*80 + "\n")
                        print("You can now run your tests:")
                        print(f"set TEST_GOOGLE_ID_TOKEN={id_token}")
                        print("./test-auth-complete.sh\n")
                        
                        # Shutdown the server
                        # Running in a new thread to allow the response to complete
                        import threading
                        threading.Thread(target=self.server.shutdown).start()
                except Exception as e:
                    self.send_response(500)
                    self.end_headers()
                    self.wfile.write(f"Error: {str(e)}".encode())
            else:
                self.send_response(400)
                self.end_headers()
                self.wfile.write(b"No code found in request")

def run():
    port = 8080
    print(f"Starting local server on port {port}...")
    print(f"Please ensure {REDIRECT_URI} is added to your Google OAuth Authorized Redirect URIs.")
    
    with socketserver.TCPServer(("", port), OAuthHandler) as httpd:
        print("Opening browser to authenticate...")
        webbrowser.open(f"http://localhost:{port}/")
        httpd.serve_forever()

if __name__ == '__main__':
    run()