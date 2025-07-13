import asyncio
import http.client
import time
from urllib.parse import urlparse
from statistics import mean, median

# Config
# URL = "https://api.paradaux.io/api/ifum/visits"
URL = "http://127.0.0.1:8080/api/visits/project/ifuckedur.mom"
TOTAL_REQUESTS = 1500
MAX_CONCURRENCY = 50

response_times = []
failures = 0

semaphore = asyncio.Semaphore(MAX_CONCURRENCY)

def sync_http_get(url):
    parsed = urlparse(url)
    conn_class = http.client.HTTPSConnection if parsed.scheme == "https" else http.client.HTTPConnection
    conn = conn_class(parsed.netloc, timeout=10)
    full_path = parsed.path or "/"
    if parsed.query:
        full_path += "?" + parsed.query
    headers = {
        "Host": parsed.hostname,
        "User-Agent": "BenchmarkClient/1.0"
    }
    try:
        start = time.perf_counter()
        conn.request("GET", full_path, headers=headers)
        response = conn.getresponse()
        response.read()
        duration = time.perf_counter() - start
        conn.close()
        return response.status, duration
    except Exception as e:
        conn.close()
        print(f"[ERROR] Exception during request: {e}")
        return None, None


async def fetch(i):
    global failures
    async with semaphore:
        loop = asyncio.get_running_loop()
        print(f"Starting request {i}")
        status, duration = await loop.run_in_executor(None, sync_http_get, URL)
        if status == 200:
            print(f"Request {i} succeeded in {duration:.4f}s")
            response_times.append(duration)
        else:
            print(f"Request {i} failed (status={status})")
            failures += 1

async def main():
    print("Starting benchmark...")
    tasks = [fetch(i) for i in range(TOTAL_REQUESTS)]
    await asyncio.gather(*tasks)

    if response_times:
        print(f"\n--- Benchmark Report for {URL} ---")
        print(f"Total Requests: {TOTAL_REQUESTS}")
        print(f"Successful: {len(response_times)}")
        print(f"Failed: {failures}")
        print(f"Average Response Time: {mean(response_times):.4f} seconds")
        print(f"Median Response Time: {median(response_times):.4f} seconds")
        print(f"Min Response Time: {min(response_times):.4f} seconds")
        print(f"Max Response Time: {max(response_times):.4f} seconds")
    else:
        print("All requests failed.")

if __name__ == "__main__":
    asyncio.run(main())
