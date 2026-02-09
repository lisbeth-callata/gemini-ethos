/**
 * Gemini Ethos - Frontend Application v3.0
 * Turismo Responsable Inteligente
 * Gemini 3 Hackathon Edition - Compatible with Marathon Agent
 */

const API_BASE = 'http://localhost:8080';
const API_V1 = `${API_BASE}/api/v1`;
const API_V3 = `${API_BASE}/api/v3`;
let currentImageData = null;
let currentAnalysisResult = null;
let cameraStream = null;
let userLocation = null;

// ==================== Initialization ====================

document.addEventListener('DOMContentLoaded', () => {
    initializeApp();
});

function initializeApp() {
    checkServerStatus();
    setupEventListeners();
    setInterval(checkServerStatus, 15000);
}

function setupEventListeners() {
    const $ = id => document.getElementById(id);
    
    // Drop zone
    $('dropZone').addEventListener('click', () => $('fileInput').click());
    $('dropZone').addEventListener('dragover', handleDragOver);
    $('dropZone').addEventListener('dragleave', handleDragLeave);
    $('dropZone').addEventListener('drop', handleDrop);
    $('fileInput').addEventListener('change', handleFileSelect);
    
    // Camera
    $('btnStartCamera').addEventListener('click', startCamera);
    $('btnCapture').addEventListener('click', capturePhoto);
    $('btnStopCamera').addEventListener('click', stopCamera);
    
    // Location
    $('btnGeolocate').addEventListener('click', getUserLocation);
    $('mapSearch').addEventListener('keypress', e => { if (e.key === 'Enter') searchLocation($('mapSearch').value); });
    
    // Analysis
    $('btnAnalyze').addEventListener('click', analyzeImage);
    $('btnNewAnalysis').addEventListener('click', resetAnalysis);
    
    // Report
    $('btnGenerateReport').addEventListener('click', generateReport);
    $('btnCloseReport').addEventListener('click', () => $('reportModal').classList.add('hidden'));
    $('btnDownloadPDF').addEventListener('click', downloadReport);
    $('btnCopyReport').addEventListener('click', copyReport);
}

// ==================== Server Status ====================

async function checkServerStatus() {
    const statusDot = document.getElementById('statusDot');
    const statusPing = document.getElementById('statusPing');
    const statusText = document.getElementById('statusText');
    
    try {
        const response = await fetch('http://localhost:8080/health');
        const data = await response.json();
        
        if (data.status === 'healthy') {
            statusDot.className = 'relative inline-flex rounded-full h-3 w-3 bg-green-500';
            statusPing.className = 'animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75';
            statusText.textContent = 'Connected';
            statusText.className = 'text-sm font-medium text-green-400';
        }
    } catch (error) {
        statusDot.className = 'relative inline-flex rounded-full h-3 w-3 bg-red-500';
        statusPing.className = 'hidden';
        statusText.textContent = 'Disconnected';
        statusText.className = 'text-sm font-medium text-red-400';
    }
}

// ==================== File Upload ====================

function handleDragOver(e) {
    e.preventDefault();
    e.currentTarget.classList.add('dragover');
}

function handleDragLeave(e) {
    e.preventDefault();
    e.currentTarget.classList.remove('dragover');
}

function handleDrop(e) {
    e.preventDefault();
    e.currentTarget.classList.remove('dragover');
    if (e.dataTransfer.files.length > 0) processFile(e.dataTransfer.files[0]);
}

function handleFileSelect(e) {
    if (e.target.files.length > 0) processFile(e.target.files[0]);
}

function processFile(file) {
    if (!file.type.startsWith('image/')) {
        showNotification('Select a valid image', 'error');
        return;
    }
    
    if (file.size > 10 * 1024 * 1024) {
        showNotification('Image must not exceed 10MB', 'error');
        return;
    }
    
    const reader = new FileReader();
    reader.onload = (e) => {
        currentImageData = {
            base64: e.target.result.split(',')[1],
            mimeType: file.type
        };
        
        document.getElementById('previewImage').src = e.target.result;
        document.getElementById('imagePreview').classList.remove('hidden');
        document.getElementById('resultsSection').classList.add('hidden');
    };
    reader.readAsDataURL(file);
}

// ==================== Camera ====================

async function startCamera() {
    try {
        cameraStream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } }
        });
        
        const video = document.getElementById('cameraFeed');
        video.srcObject = cameraStream;
        video.classList.remove('hidden');
        document.getElementById('cameraPlaceholder').classList.add('hidden');
        document.getElementById('cameraRecording').classList.remove('hidden');
        
        document.getElementById('btnStartCamera').disabled = true;
        document.getElementById('btnCapture').disabled = false;
        document.getElementById('btnStopCamera').disabled = false;
        
        showNotification('Camera started', 'success');
    } catch (error) {
        showNotification('Could not access camera', 'error');
    }
}

function capturePhoto() {
    if (!cameraStream) return;
    
    const video = document.getElementById('cameraFeed');
    const canvas = document.getElementById('cameraCanvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0);
    
    const dataUrl = canvas.toDataURL('image/jpeg', 0.9);
    currentImageData = { base64: dataUrl.split(',')[1], mimeType: 'image/jpeg' };
    
    document.getElementById('previewImage').src = dataUrl;
    document.getElementById('imagePreview').classList.remove('hidden');
    
    showNotification('Foto capturada', 'success');
}

function stopCamera() {
    if (cameraStream) {
        cameraStream.getTracks().forEach(t => t.stop());
        cameraStream = null;
    }
    
    document.getElementById('cameraFeed').classList.add('hidden');
    document.getElementById('cameraPlaceholder').classList.remove('hidden');
    document.getElementById('cameraRecording').classList.add('hidden');
    
    document.getElementById('btnStartCamera').disabled = false;
    document.getElementById('btnCapture').disabled = true;
    document.getElementById('btnStopCamera').disabled = true;
}

// ==================== Geolocation ====================

function getUserLocation() {
    if (!navigator.geolocation) {
        showNotification('Geolocation not supported', 'error');
        return;
    }
    
    const btn = document.getElementById('btnGeolocate');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> Obteniendo...';
    btn.disabled = true;
    
    navigator.geolocation.getCurrentPosition(
        async (pos) => {
            userLocation = { latitude: pos.coords.latitude, longitude: pos.coords.longitude };
            
            document.getElementById('geoCoords').textContent = 
                `${userLocation.latitude.toFixed(6)}, ${userLocation.longitude.toFixed(6)}`;
            document.getElementById('geoResult').classList.remove('hidden');
            
            try {
                const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${userLocation.latitude}&lon=${userLocation.longitude}&zoom=12`);
                const data = await res.json();
                if (data.display_name) {
                    document.getElementById('geoAddress').textContent = data.display_name;
                }
            } catch (e) {}
            
            btn.innerHTML = '<i class="fas fa-check mr-2"></i> Location obtained';
            setTimeout(() => {
                btn.innerHTML = '<i class="fas fa-crosshairs mr-2"></i> Use my GPS location';
                btn.disabled = false;
            }, 2000);
            
            showNotification('Location obtained', 'success');
        },
        (err) => {
            btn.innerHTML = '<i class="fas fa-crosshairs mr-2"></i> Use my GPS location';
            btn.disabled = false;
            showNotification('Error obtaining location', 'error');
        },
        { enableHighAccuracy: true, timeout: 10000 }
    );
}

async function searchLocation(query) {
    if (!query.trim()) return;
    
    try {
        const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`);
        const data = await res.json();
        
        if (data.length > 0) {
            userLocation = { latitude: parseFloat(data[0].lat), longitude: parseFloat(data[0].lon) };
            document.getElementById('geoCoords').textContent = 
                `${userLocation.latitude.toFixed(6)}, ${userLocation.longitude.toFixed(6)}`;
            document.getElementById('geoAddress').textContent = data[0].display_name;
            document.getElementById('geoResult').classList.remove('hidden');
            showNotification('Location found', 'success');
        } else {
            showNotification('Location not found', 'error');
        }
    } catch (e) {
        showNotification('Search error', 'error');
    }
}

// ==================== Analysis ====================

async function analyzeImage() {
    if (!currentImageData) {
        showNotification('Upload or capture an image first', 'error');
        return;
    }
    
    document.getElementById('imagePreview').classList.add('hidden');
    document.getElementById('loadingSection').classList.remove('hidden');
    document.getElementById('resultsSection').classList.add('hidden');
    
    const messages = ['Processing image...', 'Analyzing with Gemini 3...', 'Detecting behaviors...', 'Evaluating impact...', 'Generating recommendations...'];
    let i = 0;
    const msgInterval = setInterval(() => {
        document.getElementById('loadingMessage').textContent = messages[++i % messages.length];
    }, 1500);
    
    try {
        const location = document.getElementById('parkSelector').value || 'general';
        
        const body = {
            image: currentImageData.base64,
            mimeType: currentImageData.mimeType,
            location: location,
            parkId: location,
            timestamp: new Date().toISOString()
        };
        
        if (userLocation) {
            body.latitude = userLocation.latitude;
            body.longitude = userLocation.longitude;
        }
        
        // Use the v1 analyze endpoint (compatible with Marathon Agent)
        const response = await fetch(`${API_V1}/analyze`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        
        clearInterval(msgInterval);
        
        if (!response.ok) throw new Error(`Error ${response.status}`);
        
        const result = await response.json();
        currentAnalysisResult = result;
        displayResults(result);
        
        // Auto-log to active Marathon missions so every park event is tracked
        logToMarathonMissions(body);
        
    } catch (error) {
        clearInterval(msgInterval);
        showNotification(`Error: ${error.message}`, 'error');
        document.getElementById('loadingSection').classList.add('hidden');
        document.getElementById('imagePreview').classList.remove('hidden');
    }
}

/**
 * Auto-log analysis to Marathon Agent missions.
 * If no active mission exists, one is auto-created so every park event is tracked.
 * Runs in the background — failures are silent so the main UI is not affected.
 */
async function logToMarathonMissions(analysisBody) {
    // Map park IDs to display names for better UX
    const parkNames = {
        'galapagos': 'Galápagos Islands, Ecuador',
        'machu_picchu': 'Machu Picchu, Peru',
        'amazon': 'Amazon Rainforest',
        'patagonia': 'Patagonia, Argentina',
        'costa_rica': 'Costa Rica',
        'auto_detect': 'Auto-detected Location',
        'general': 'General Area'
    };
    const parkId = analysisBody.location || analysisBody.parkId || 'general';
    const locationName = parkNames[parkId] || parkId;

    try {
        // 1. Check for active missions
        const missionsRes = await fetch(`${API_V3}/missions`);
        if (!missionsRes.ok) return;
        const data = await missionsRes.json();
        const missions = data.missions || [];
        let activeMissions = missions.filter(m => m.status === 'ACTIVE');

        // 2. Auto-create a mission if none exist
        if (activeMissions.length === 0) {
            const startRes = await fetch(`${API_V3}/mission/start`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    parkId: parkId,
                    type: 'REAL_TIME_PATROL',
                    description: 'Autonomous patrol of ' + locationName,
                    durationHours: 8
                })
            });
            if (!startRes.ok) { console.warn('Could not auto-create Marathon mission'); return; }
            const startData = await startRes.json();
            if (startData.mission) {
                activeMissions = [startData.mission];
                console.log(`🚀 Auto-created Marathon mission: ${startData.mission.missionId}`);
            }
        }

        // 3. Send analysis to every active mission
        const missionBody = {
            image: analysisBody.image,
            mimeType: analysisBody.mimeType,
            location: locationName
        };
        if (analysisBody.latitude) {
            missionBody.geoLocation = { latitude: analysisBody.latitude, longitude: analysisBody.longitude };
        }

        const promises = activeMissions.map(m =>
            fetch(`${API_V3}/mission/${m.missionId}/analyze`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(missionBody)
            }).catch(() => {})
        );

        await Promise.allSettled(promises);
        console.log(`📡 Analysis logged to ${activeMissions.length} Marathon mission(s)`);
    } catch (e) {
        console.warn('Marathon logging skipped:', e.message);
    }
}

function displayResults(result) {
    document.getElementById('loadingSection').classList.add('hidden');
    document.getElementById('resultsSection').classList.remove('hidden');
    
    // Map risk level - handle both formats
    let riskLevel = result.overallRiskLevel || result.riskLevel || 'LOW';
    if (typeof riskLevel === 'object') riskLevel = riskLevel.name || 'LOW';
    riskLevel = riskLevel.toUpperCase();
    
    const riskMap = { 'LOW': 'LOW', 'MEDIUM': 'MEDIUM', 'HIGH': 'HIGH', 'CRITICAL': 'CRITICAL' };
    const riskClasses = { 'LOW': 'risk-low', 'MEDIUM': 'risk-medium', 'HIGH': 'risk-high', 'CRITICAL': 'risk-critical' };
    const riskIcons = { 'LOW': 'fa-leaf', 'MEDIUM': 'fa-exclamation-circle', 'HIGH': 'fa-exclamation-triangle', 'CRITICAL': 'fa-skull-crossbones' };
    
    document.getElementById('riskLevel').textContent = riskMap[riskLevel] || riskLevel;
    document.getElementById('riskDescription').textContent = result.summary || 'Analysis completed.';
    document.getElementById('riskCard').className = `rounded-3xl p-10 text-white ${riskClasses[riskLevel] || 'risk-low'}`;
    document.getElementById('riskIcon').innerHTML = `<i class="fas ${riskIcons[riskLevel] || 'fa-leaf'} text-6xl"></i>`;
    
    // Reasoning Process
    const rp = result.reasoningProcess;
    if (rp) {
        let html = '';
        
        // Handle both object and string formats
        if (typeof rp === 'object') {
            if (rp.contextualAssessment) {
                html += `<div class="p-5 rounded-2xl bg-purple-500/10 border border-purple-500/20 mb-3">
                    <p class="text-purple-400 text-xs font-semibold mb-2">🔍 Contextual Assessment</p>
                    <p class="text-gray-300">"${rp.contextualAssessment}"</p>
                </div>`;
            }
            if (rp.riskJustification) {
                html += `<div class="p-5 rounded-2xl bg-warm-500/10 border border-warm-500/20 mb-3">
                    <p class="text-warm-400 text-xs font-semibold mb-2">⚠️ Risk Justification</p>
                    <p class="text-gray-300">"${rp.riskJustification}"</p>
                </div>`;
            }
            if (rp.inferenceChain && rp.inferenceChain.length > 0) {
                html += `<div class="p-5 rounded-2xl bg-accent-500/10 border border-accent-500/20">
                    <p class="text-accent-400 text-xs font-semibold mb-2">🧠 Inference Chain</p>
                    <ul class="text-gray-300 text-sm space-y-1">
                        ${rp.inferenceChain.map(i => `<li>→ ${i}</li>`).join('')}
                    </ul>
                </div>`;
            }
            if (rp.visualObservations && rp.visualObservations.length > 0) {
                html += `<div class="p-5 rounded-2xl bg-primary-500/10 border border-primary-500/20 mt-3">
                    <p class="text-primary-400 text-xs font-semibold mb-2">👁️ Visual Observations</p>
                    <ul class="text-gray-300 text-sm space-y-2">
                        ${rp.visualObservations.map(vo => 
                            `<li><strong>${vo.element || 'Element'}:</strong> ${vo.description || ''}</li>`
                        ).join('')}
                    </ul>
                </div>`;
            }
        }
        
        if (html) {
            document.getElementById('reasoningContent').innerHTML = html;
        } else {
            document.getElementById('thinkingText').textContent = 'Analysis completed successfully.';
        }
    } else {
        document.getElementById('thinkingText').textContent = result.summary || 'Analysis completed.';
    }
    
    // Causal Analysis
    const ca = result.causalAnalysis;
    if (ca) {
        document.getElementById('rootCause').textContent = ca.primaryCause || ca.rootCause || 'Not identified';
        
        const immEffects = document.getElementById('immediateEffects');
        immEffects.innerHTML = '';
        const shortTerm = ca.shortTermConsequence ? [ca.shortTermConsequence] : (ca.immediateEffects || []);
        shortTerm.forEach(e => {
            const li = document.createElement('li');
            li.textContent = `• ${e}`;
            immEffects.appendChild(li);
        });
        
        const longEffects = document.getElementById('longTermEffects');
        longEffects.innerHTML = '';
        const longTerm = ca.longTermConsequence ? [ca.longTermConsequence] : (ca.longTermEffects || []);
        longTerm.forEach(e => {
            const li = document.createElement('li');
            li.textContent = `• ${e}`;
            longEffects.appendChild(li);
        });
    } else {
        document.getElementById('rootCause').textContent = 'No problematic cause detected';
    }
    
    // Behaviors
    const behaviorsGrid = document.getElementById('behaviorsGrid');
    behaviorsGrid.innerHTML = '';
    const behaviors = result.detectedBehaviors || [];
    
    if (behaviors.length === 0) {
        behaviorsGrid.innerHTML = '<p class="text-gray-400 col-span-2">No specific behaviors detected.</p>';
    } else {
        behaviors.forEach(b => {
            const risk = (b.riskLevel || 'LOW').toString().toUpperCase();
            const isPositive = risk === 'LOW';
            const card = document.createElement('div');
            card.className = `p-5 rounded-2xl ${isPositive ? 'bg-primary-500/10 border border-primary-500/20' : 'bg-danger-500/10 border border-danger-500/20'}`;
            card.innerHTML = `
                <div class="flex items-start gap-4">
                    <div class="w-10 h-10 rounded-xl ${isPositive ? 'bg-primary-500/20' : 'bg-danger-500/20'} flex items-center justify-center flex-shrink-0">
                        <i class="fas ${isPositive ? 'fa-check text-primary-400' : 'fa-times text-danger-400'}"></i>
                    </div>
                    <div>
                        <h4 class="font-semibold ${isPositive ? 'text-primary-300' : 'text-danger-300'}">${b.behaviorType || 'Behavior'}</h4>
                        <p class="text-gray-400 text-sm mt-1">${b.description || ''}</p>
                        ${b.location ? `<p class="text-gray-500 text-xs mt-2"><i class="fas fa-map-marker-alt mr-1"></i>${b.location}</p>` : ''}
                    </div>
                </div>
            `;
            behaviorsGrid.appendChild(card);
        });
    }
    
    // Guidelines
    const guidelinesGrid = document.getElementById('guidelinesGrid');
    guidelinesGrid.innerHTML = '';
    const guidelines = result.guidelines || [];
    
    if (guidelines.length === 0) {
        guidelinesGrid.innerHTML = '<p class="text-gray-400 col-span-2">No additional guidelines.</p>';
    } else {
        guidelines.forEach(g => {
            const card = document.createElement('div');
            card.className = 'p-5 rounded-2xl bg-accent-500/10 border border-accent-500/20';
            card.innerHTML = `
                <div class="flex items-start gap-4">
                    <div class="w-10 h-10 rounded-xl bg-accent-500/20 flex items-center justify-center flex-shrink-0">
                        <i class="fas fa-compass text-accent-400"></i>
                    </div>
                    <div>
                        <h4 class="font-semibold text-accent-300">${g.category || 'Recommendation'}</h4>
                        <p class="text-gray-400 text-sm mt-1">${g.guideline || g.recommendation || ''}</p>
                        ${g.culturalContext ? `<p class="text-gray-500 text-xs mt-2 italic">${g.culturalContext}</p>` : ''}
                    </div>
                </div>
            `;
            guidelinesGrid.appendChild(card);
        });
    }
    
    // Show report button for high/critical
    if (riskLevel === 'HIGH' || riskLevel === 'CRITICAL') {
        document.getElementById('reportSection').classList.remove('hidden');
    } else {
        document.getElementById('reportSection').classList.add('hidden');
    }
}

// ==================== Report ====================

function generateReport() {
    if (!currentAnalysisResult) return;
    
    const r = currentAnalysisResult;
    const now = new Date().toLocaleString('en-US');
    const id = `ETHOS-${Date.now().toString(36).toUpperCase()}`;
    
    document.getElementById('reportContent').innerHTML = `
        <div class="space-y-6 text-gray-300">
            <div class="text-center pb-4 border-b border-white/10">
                <h2 class="text-2xl font-bold text-white">ENVIRONMENTAL INCIDENT REPORT</h2>
                <p class="text-gray-400 text-sm">Gemini Ethos - ${now}</p>
                <p class="text-xs text-gray-500 mt-1 font-mono">${id}</p>
            </div>
            
            <div class="grid grid-cols-2 gap-4">
                <div class="p-4 rounded-xl bg-white/5">
                    <p class="text-xs text-gray-500">Risk Level</p>
                    <p class="font-bold text-danger-400">${r.overallRiskLevel || 'HIGH'}</p>
                </div>
                <div class="p-4 rounded-xl bg-white/5">
                    <p class="text-xs text-gray-500">Location</p>
                    <p class="font-medium">${document.getElementById('parkSelector').selectedOptions[0]?.text || 'Not specified'}</p>
                </div>
            </div>
            
            <div>
                <h4 class="font-semibold text-white mb-2">Summary</h4>
                <p>${r.summary || 'No summary'}</p>
            </div>
            
            <div>
                <h4 class="font-semibold text-white mb-2">Detected Behaviors</h4>
                <ul class="list-disc list-inside space-y-1">
                    ${(r.detectedBehaviors || []).map(b => `<li>${b.description || b.behaviorType}</li>`).join('') || '<li>None</li>'}
                </ul>
            </div>
            
            <div>
                <h4 class="font-semibold text-white mb-2">Recommended Actions</h4>
                <ul class="list-disc list-inside space-y-1">
                    ${(r.immediateActions || []).map(a => `<li>${a}</li>`).join('') || '<li>Follow ethical guidelines</li>'}
                </ul>
            </div>
            
            <p class="text-xs text-gray-500 text-center pt-4 border-t border-white/10">
                Report automatically generated by Gemini Ethos - Powered by Vertex AI
            </p>
        </div>
    `;
    
    document.getElementById('reportModal').classList.remove('hidden');
}

function downloadReport() {
    const content = document.getElementById('reportContent').innerText;
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Report_Ethos_${Date.now()}.txt`;
    a.click();
    URL.revokeObjectURL(url);
    showNotification('Report downloaded', 'success');
}

function copyReport() {
    const content = document.getElementById('reportContent').innerText;
    navigator.clipboard.writeText(content).then(() => showNotification('Report copied', 'success'));
}

// ==================== Utilities ====================

function resetAnalysis() {
    currentImageData = null;
    currentAnalysisResult = null;
    document.getElementById('previewImage').src = '';
    document.getElementById('imagePreview').classList.add('hidden');
    document.getElementById('resultsSection').classList.add('hidden');
    document.getElementById('fileInput').value = '';
    showNotification('Ready for new analysis', 'info');
}

function showNotification(message, type = 'info') {
    const colors = { success: 'bg-green-600', error: 'bg-red-600', info: 'bg-blue-600', warning: 'bg-amber-600' };
    const icons = { success: 'fa-check-circle', error: 'fa-times-circle', info: 'fa-info-circle', warning: 'fa-exclamation-circle' };
    
    const notification = document.createElement('div');
    notification.className = `fixed bottom-6 right-6 ${colors[type]} text-white px-6 py-4 rounded-2xl shadow-2xl flex items-center gap-3 z-50 fade-in`;
    notification.innerHTML = `<i class="fas ${icons[type]} text-lg"></i><span class="font-medium">${message}</span>`;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.opacity = '0';
        notification.style.transform = 'translateY(20px)';
        notification.style.transition = 'all 0.3s ease';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}
