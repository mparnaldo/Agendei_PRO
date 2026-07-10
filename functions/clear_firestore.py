import json
import time
import base64
import urllib.request
import urllib.parse
import urllib.error
import subprocess
import os

# 1. Carregar credenciais
key_path = '/home/arnaldo/Documentos/Android-PRJ/Agendei_PRO/functions/firebase-admin-key.json'
with open(key_path, 'r') as f:
    creds = json.load(f)

project_id = creds['project_id']
client_email = creds['client_email']
private_key = creds['private_key']

# 2. Gerar JWT para obter token do Google OAuth
header = {"alg": "RS256", "typ": "JWT"}
header_b64 = base64.urlsafe_b64encode(json.dumps(header).encode('utf-8')).decode('utf-8').rstrip('=')

now = int(time.time())
payload = {
    "iss": client_email,
    "scope": "https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/userinfo.email",
    "aud": "https://oauth2.googleapis.com/token",
    "exp": now + 3600,
    "iat": now
}
payload_b64 = base64.urlsafe_b64encode(json.dumps(payload).encode('utf-8')).decode('utf-8').rstrip('=')

signing_input = f"{header_b64}.{payload_b64}"

# Salvar a chave privada temporariamente para assinar com OpenSSL
key_file = 'temp_private_key.pem'
with open(key_file, 'w') as f:
    f.write(private_key)

try:
    # Assinar usando OpenSSL do sistema
    proc = subprocess.Popen(
        ['openssl', 'dgst', '-sha256', '-sign', key_file],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )
    signature, err = proc.communicate(input=signing_input.encode('utf-8'))
    if proc.returncode != 0:
        raise Exception(f"Erro no OpenSSL ao assinar JWT: {err.decode('utf-8')}")
finally:
    if os.path.exists(key_file):
        os.remove(key_file)

signature_b64 = base64.urlsafe_b64encode(signature).decode('utf-8').rstrip('=')
jwt = f"{signing_input}.{signature_b64}"

# 3. Obter Token de Acesso OAuth2
token_url = "https://oauth2.googleapis.com/token"
data = urllib.parse.urlencode({
    "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
    "assertion": jwt
}).encode('utf-8')

req = urllib.request.Request(token_url, data=data, headers={"Content-Type": "application/x-www-form-urlencoded"})
try:
    with urllib.request.urlopen(req) as resp:
        token_data = json.loads(resp.read().decode('utf-8'))
        access_token = token_data['access_token']
except Exception as e:
    print("Erro ao obter token de acesso do Google OAuth:", e)
    exit(1)

print("Autenticado com sucesso via OAuth2!")

# 4. Requisições REST para gerenciar Firestore
def request_firestore(url, method='GET'):
    req = urllib.request.Request(
        url,
        method=method,
        headers={
            "Authorization": f"Bearer {access_token}",
            "Content-Type": "application/json"
        }
    )
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        if e.code == 404 and method == 'GET':
            return {}
        print(f"Erro HTTP {e.code} para {url}: {e.read().decode('utf-8')}")
        return None
    except Exception as e:
        print(f"Erro na requisição para {url}: {e}")
        return None

base_url = f"https://firestore.googleapis.com/v1/projects/{project_id}/databases/(default)/documents"

# Coleções simples para limpar
collections = ['appointments', 'waiting_list', 'user_bindings', 'profiles', 'notifications']

for col in collections:
    print(f"Limpando coleção: {col}...")
    url = f"{base_url}/{col}"
    data = request_firestore(url)
    if data and 'documents' in data:
        for doc in data['documents']:
            doc_name = doc['name']
            print(f"Deletando documento: {doc_name}...")
            request_firestore(f"https://firestore.googleapis.com/v1/{doc_name}", method='DELETE')

# Limpar salons e suas subcoleções de serviços
print("Buscando salões para limpar subcoleções de serviços...")
salons_data = request_firestore(f"{base_url}/salons")
if salons_data and 'documents' in salons_data:
    for salon_doc in salons_data['documents']:
        salon_name = salon_doc['name']
        salon_id = salon_name.split('/')[-1]
        
        # Limpar subcoleção 'services'
        print(f"Limpando subcoleção 'services' para o salão {salon_id}...")
        services_data = request_firestore(f"https://firestore.googleapis.com/v1/{salon_name}/services")
        if services_data and 'documents' in services_data:
            for service_doc in services_data['documents']:
                service_name = service_doc['name']
                print(f"Deletando serviço: {service_name}...")
                request_firestore(f"https://firestore.googleapis.com/v1/{service_name}", method='DELETE')
                
        # Deletar o salão
        print(f"Deletando salão: {salon_name}...")
        request_firestore(f"https://firestore.googleapis.com/v1/{salon_name}", method='DELETE')

print("Banco de dados Firestore totalmente limpo e zerado!")
