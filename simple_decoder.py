import requests
import re

def decode_secret_message(url):
    response = requests.get(url)
    html = response.text
    
    td_pattern = r'<td[^>]*>(.*?)</td>'
    all_tds = re.findall(td_pattern, html, re.DOTALL)
    
    clean_tds = []
    for td in all_tds:
        clean = re.sub(r'<[^>]+>', '', td).strip()
        if clean:
            clean_tds.append(clean)
    
    data = []
    max_x = max_y = 0
    
    for i in range(0, len(clean_tds) - 2, 3):
        try:
            x = int(clean_tds[i])
            char = clean_tds[i + 1]
            y = int(clean_tds[i + 2])
            data.append((x, char, y))
            max_x = max(max_x, x)
            max_y = max(max_y, y)
        except:
            continue
    
    grid = [[' ' for _ in range(max_x + 1)] for _ in range(max_y + 1)]
    
    for x, char, y in data:
        grid[y][x] = char
    
    for row in grid:
        print(''.join(row))

if __name__ == "__main__":
    url = "https://docs.google.com/document/d/e/2PACX-1vRPzbNQcx5UriHSbZ-9vmsTow_R6RRe7eyAU60xIF9Dlz-vaHiHNO2TKgDi7jy4ZpTpNqM7EvEcfr_p/pub"
    decode_secret_message(url)