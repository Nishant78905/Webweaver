async function startCrawl() {
    let url = document.getElementById('startUrl').value.trim();
    let topic = document.getElementById('topic').value.trim();
    const topK = document.getElementById('topK').value;
    const depth = document.getElementById('depth').value;
    const maxPages = document.getElementById('maxPages').value;
    const resultLimit = document.getElementById('resultLimit').value; // NEW

    if (!url || !topic) {
        alert("Please enter both a starting URL and a Target Topic.");
        return;
    }

    // UI Updates during loading
    document.getElementById('loading').classList.remove('hidden');
    document.getElementById('results').classList.add('hidden');
    document.getElementById('crawlBtn').disabled = true;

    try {
        // API call with all Phase 5 parameters
        const apiUrl = `/api/crawler/run?startUrl=${encodeURIComponent(url)}&topic=${encodeURIComponent(topic)}&topK=${topK}&depth=${depth}&maxPages=${maxPages}&resultLimit=${resultLimit}`;

        const response = await fetch(apiUrl);

        if (!response.ok) throw new Error("Backend server error");
        const data = await response.json();

        populateDashboard(data);

        document.getElementById('loading').classList.add('hidden');
        document.getElementById('results').classList.remove('hidden');
    } catch (error) {
        alert("Error: " + error.message);
        document.getElementById('loading').classList.add('hidden');
    } finally {
        document.getElementById('crawlBtn').disabled = false;
    }
}

function populateDashboard(data) {
    document.getElementById('totalVisited').innerText = data.totalVisited;
    const tableBody = document.getElementById('rankBody');
    tableBody.innerHTML = "";

    data.topPages.forEach((page, index) => {
        const row = `<tr>
            <td class="rank-col">#${index + 1}</td>
            <td class="url-col"><a href="${page.url}" target="_blank">${shortenUrl(page.url)}</a></td>
            <td class="score-col"><span class="badge">${page.rank.toFixed(4)}</span></td>
        </tr>`;
        tableBody.innerHTML += row;
    });

    drawGraph(data.graph);
}

function drawGraph(adjacencyList) {
    const nodesArray = [];
    const edgesArray = [];
    const nodeSet = new Set();

    for (const sourceNode in adjacencyList) {
        if (!nodeSet.has(sourceNode)) {
            nodesArray.push({ id: sourceNode, label: shortenUrl(sourceNode), title: sourceNode });
            nodeSet.add(sourceNode);
        }

        const targetNodes = adjacencyList[sourceNode];
        if (targetNodes) {
            targetNodes.forEach(targetNode => {
                if (!nodeSet.has(targetNode)) {
                    nodesArray.push({ id: targetNode, label: shortenUrl(targetNode), title: targetNode });
                    nodeSet.add(targetNode);
                }
                edgesArray.push({ from: sourceNode, to: targetNode, arrows: 'to' });
            });
        }
    }

    const container = document.getElementById('networkGraph');
    const data = { nodes: new vis.DataSet(nodesArray), edges: new vis.DataSet(edgesArray) };
    const options = {
        nodes: {
            shape: 'dot',
            size: 14,
            color: { background: '#3b82f6', border: '#2563eb' },
            font: { size: 12, face: 'Inter' },
            borderWidth: 2,
            shadow: true
        },
        edges: {
            color: '#cbd5e1',
            smooth: { type: 'continuous' },
            width: 1.5
        },
        physics: {
            stabilization: false,
            barnesHut: { springLength: 120, avoidOverlap: 0.2 }
        },
        interaction: { hover: true }
    };

    new vis.Network(container, data, options);
}

function shortenUrl(url) {
    try {
        const urlObj = new URL(url);
        let path = urlObj.pathname + urlObj.search;
        if (path === "/" || path === "") return urlObj.hostname;
        return path.length > 35 ? path.substring(0, 35) + "..." : path;
    } catch {
        return url;
    }
}