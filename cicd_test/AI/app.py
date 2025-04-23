from flask import Flask
import numpy as np
import pandas as pd
import os

app = Flask(__name__)

@app.route('/')
def hello():
    # NumPy와 Pandas를 사용하여 간단한 작업 수행
    arr = np.array([1, 2, 3, 4, 5])
    df = pd.DataFrame({'numbers': arr})
    
    return f"""
    <h1>uv로 설치된 패키지 테스트</h1>
    <p>NumPy 배열: {arr}</p>
    <p>Pandas DataFrame:</p>
    <pre>{df.to_html()}</pre>
    """

if __name__ == '__main__':
    # 도커 환경에서 실행
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 8000))) 