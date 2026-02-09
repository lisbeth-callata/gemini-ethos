/**
 * Marathon Agent Dashboard - Frontend Application v3.0
 * Gemini 3 Hackathon Edition
 */
const API_BASE = window.location.origin;
const API_V3 = API_BASE + '/api/v3';
const CLIENT_ID = 'dashboard-' + Math.random().toString(36).substring(2, 10);
let activeMissions = new Map();
let thoughts = [];
let incidents = [];
let currentImageData = null;
let lastAnalysisResult = null;

document.addEventListener('DOMContentLoaded', () => {
    console.log('Marathon Agent Dashboard v3.0 initializing...');
    checkServerStatus();
    setupEventListeners();
    loadInitialData();
    setInterval(checkServerStatus, 10000);
    setInterval(refreshStats, 5000);
    setInterval(pollEvents, 3000);
});

function setupEventListeners() {
    const d = id => document.getElementById(id);
    const dropZone = d('quickDropZone');
    if (dropZone) {
        dropZone.addEventListener('click', () => d('quickFileInput').click());
        dropZone.addEventListener('dragover', handleDragOver);
        dropZone.addEventListener('dragleave', handleDragLeave);
        dropZone.addEventListener('drop', handleDrop);
        d('quickFileInput').addEventListener('change', handleFileSelect);
    }
    const btn = d('btnQuickAnalyze');
    if (btn) btn.addEventListener('click', analyzeQuickImage);
}

async function loadInitialData() {
    try { await refreshMissions(); await refreshStats(); } catch(e) { console.error('Init error:', e); }
}

async function checkServerStatus() {
    const statusDot = document.getElementById('statusDot');
    const statusPing = document.getElementById('statusPing');
    const statusText = document.getElementById('statusText');
    try {
        const r = await fetch(API_BASE + '/health');
        const d = await r.json();
        if (d.status === 'healthy') {
            statusDot.className = 'relative inline-flex rounded-full h-3 w-3 bg-green-500';
            statusPing.className = 'animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75';
            statusText.textContent = 'Marathon Agent Connected';
            statusText.className = 'text-sm font-medium text-green-400';
        }
    } catch(e) {
        statusDot.className = 'relative inline-flex rounded-full h-3 w-3 bg-red-500';
        statusPing.className = 'hidden';
        statusText.textContent = 'Disconnected';
        statusText.className = 'text-sm font-medium text-red-400';
    }
}

async function refreshMissions() {
    try {
        const r = await fetch(API_V3 + '/missions');
        const d = await r.json();
        if (d.missions) {
            activeMissions.clear();
            d.missions.forEach(m => activeMissions.set(m.missionId, m));
            renderMissionsList();
            updateMissionSelect();
        }
    } catch(e) { console.error('Missions error:', e); }
}

function renderMissionsList() {
    const c = document.getElementById('missionsList');
    if (activeMissions.size === 0) {
        c.innerHTML = '<div class="text-center py-12 text-gray-500"><i class="fas fa-binoculars text-4xl mb-4 opacity-50"></i><p class="font-medium">No active missions</p><p class="text-sm mt-1">Start a patrol to begin monitoring</p><button onclick="openNewMissionModal()" class="mt-4 gradient-eco text-white px-4 py-2 rounded-lg text-sm font-semibold"><i class="fas fa-plus mr-2"></i>Start First Mission</button></div>';
        return;
    }
    let h = '<div class="space-y-4">';
    activeMissions.forEach((m, id) => {
        const sc = getStatusClass(m.status);
        const si = getStatusIcon(m.status);
        const p = calculateProgress(m);
        h += '<div class="p-4 rounded-xl bg-white/5 border border-white/10 hover:border-white/20 transition-all cursor-pointer hover-lift" onclick="openMissionDetail(\'' + id + '\')">';
        h += '<div class="flex items-center justify-between mb-3"><div class="flex items-center gap-3"><div class="w-10 h-10 rounded-lg ' + sc + ' flex items-center justify-center"><i class="fas ' + si + ' text-white"></i></div><div><p class="font-semibold text-white">' + getParkDisplayName(m.parkId) + '</p><p class="text-xs text-gray-500 font-mono">' + id + '</p></div></div><span class="text-xs font-medium px-2 py-1 rounded-full ' + sc + ' text-white">' + m.status + '</span></div>';
        h += '<div class="flex items-center justify-between text-sm mb-2"><span class="text-gray-400">Progress</span><span class="text-white font-medium">' + p + '%</span></div>';
        h += '<div class="w-full h-2 bg-white/10 rounded-full overflow-hidden"><div class="h-full gradient-eco transition-all" style="width:' + p + '%"></div></div>';
        h += '<div class="grid grid-cols-4 gap-4 mt-4 pt-4 border-t border-white/10 text-center"><div><p class="text-lg font-bold text-white">' + (m.imagesAnalyzed||0) + '</p><p class="text-xs text-gray-500">Images</p></div><div><p class="text-lg font-bold text-white">' + (m.incidentsDetected||0) + '</p><p class="text-xs text-gray-500">Incidents</p></div><div><p class="text-lg font-bold text-white">' + (m.thoughtChainLength||0) + '</p><p class="text-xs text-gray-500">Thoughts</p></div><div><p class="text-lg font-bold text-white">' + (m.selfCorrections||0) + '</p><p class="text-xs text-gray-500">Corrections</p></div></div>';
        h += '</div>';
    });
    h += '</div>';
    c.innerHTML = h;
}

function getParkDisplayName(id) {
    var n = {
        'galapagos': '🐢 Galápagos Islands',
        'machu_picchu': '🏔️ Machu Picchu',
        'machu-picchu': '🏔️ Machu Picchu',
        'amazon': '🌳 Amazon Rainforest',
        'patagonia': '🦙 Patagonia',
        'costa_rica': '🦜 Costa Rica',
        'costa-rica': '🦜 Costa Rica',
        'auto_detect': '🤖 Auto-detected',
        'general': '🌍 General Patrol'
    };
    return n[id] || id;
}

function updateMissionSelect() {
    var s = document.getElementById('missionSelect');
    if (!s) return;
    s.innerHTML = '<option value="">Select active mission...</option>';
    activeMissions.forEach((m, id) => {
        if (m.status === 'ACTIVE') {
            var o = document.createElement('option');
            o.value = id;
            o.textContent = getParkDisplayName(m.parkId) + ' (' + id + ')';
            s.appendChild(o);
        }
    });
}

function getStatusClass(s) { return s==='ACTIVE'?'status-active':s==='PAUSED'?'status-paused':s==='COMPLETED'?'status-completed':s==='ABORTED'?'status-aborted':'bg-gray-600'; }
function getStatusIcon(s) { return s==='ACTIVE'?'fa-satellite-dish':s==='PAUSED'?'fa-pause':s==='COMPLETED'?'fa-check':s==='ABORTED'?'fa-times':'fa-question'; }

function calculateProgress(m) {
    if (!m.elapsedTime || !m.elapsedTime.startsWith('PT')) return 0;
    var ms = 0;
    var hm = m.elapsedTime.match(/(\d+)H/); var mm = m.elapsedTime.match(/(\d+)M/); var sm = m.elapsedTime.match(/(\d+(?:\.\d+)?)S/);
    if (hm) ms += parseInt(hm[1]) * 3600000; if (mm) ms += parseInt(mm[1]) * 60000; if (sm) ms += parseFloat(sm[1]) * 1000;
    return Math.min(100, Math.round((ms / (4*3600000)) * 100));
}

function openNewMissionModal() { document.getElementById('newMissionModal').classList.remove('hidden'); }
function closeNewMissionModal() { document.getElementById('newMissionModal').classList.add('hidden'); document.getElementById('newMissionForm').reset(); }

async function startNewMission(event) {
    event.preventDefault();
    var parkId = document.getElementById('parkId').value;
    var missionType = document.querySelector('input[name="missionType"]:checked').value;
    var durationHours = parseInt(document.getElementById('durationHours').value);
    var description = document.getElementById('missionDescription').value;
    if (!parkId) { showToast('Please select a protected area', 'error'); return; }
    var btn = event.target.querySelector('button[type="submit"]');
    btn.disabled = true; btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> Launching...';
    try {
        var r = await fetch(API_V3 + '/mission/start', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ parkId: parkId, type: missionType, durationHours: durationHours, description: description || (missionType + ' patrol of ' + parkId) }) });
        var d = await r.json();
        if (d.success) { showToast('Mission launched: ' + d.mission.missionId, 'success'); closeNewMissionModal(); await refreshMissions(); addThought({ level: 'HIGH', content: 'New mission started for ' + getParkDisplayName(parkId) + '. Agent initializing autonomous patrol with Gemini 3 reasoning...', timestamp: new Date().toISOString() }); }
        else { showToast(d.error || 'Failed to start mission', 'error'); }
    } catch(e) { showToast('Failed to connect to server', 'error'); }
    finally { btn.disabled = false; btn.innerHTML = '<i class="fas fa-rocket mr-2"></i> Launch Mission'; }
}

async function openMissionDetail(missionId) {
    var modal = document.getElementById('missionDetailModal');
    var content = document.getElementById('missionDetailContent');
    modal.classList.remove('hidden');
    content.innerHTML = '<div class="text-center py-8"><i class="fas fa-spinner fa-spin text-3xl text-gray-500"></i></div>';
    try {
        var [mr, tr, ir, imgr] = await Promise.all([fetch(API_V3+'/mission/'+missionId), fetch(API_V3+'/mission/'+missionId+'/thoughts'), fetch(API_V3+'/mission/'+missionId+'/incidents'), fetch(API_V3+'/mission/'+missionId+'/images')]);
        var md = await mr.json(); var td = await tr.json(); var id = await ir.json(); var imgd = await imgr.json();
        var mission = md.mission || md;
        document.getElementById('detailMissionTitle').textContent = getParkDisplayName(mission.parkId) + ' Patrol';
        document.getElementById('detailMissionId').textContent = mission.missionId || missionId;
        content.innerHTML = renderMissionDetail(mission, md, td.thoughts||[], id.incidents||[], imgd.images||[]);
    } catch(e) { content.innerHTML = '<div class="text-center py-8 text-red-400"><i class="fas fa-exclamation-triangle text-3xl mb-3"></i><p>Failed to load details</p></div>'; }
}

function renderMissionDetail(mission, full, thoughts, incidents, images) {
    var mid = mission.missionId;
    var sc = getStatusClass(mission.status);
    var h = '<div class="space-y-6">';
    h += '<div class="flex items-center justify-between p-4 rounded-xl bg-white/5"><div class="flex items-center gap-4"><span class="px-4 py-2 rounded-lg ' + sc + ' text-white font-semibold">' + mission.status + '</span><div><p class="text-sm text-gray-400">Elapsed</p><p class="text-white font-mono">' + (mission.elapsedTime||'-') + '</p></div></div>';
    h += '<div class="flex gap-2">';
    if (mission.status==='ACTIVE') h += '<button onclick="pauseMission(\''+mid+'\')" class="px-4 py-2 rounded-lg bg-yellow-600/20 text-yellow-400 hover:bg-yellow-600/30"><i class="fas fa-pause mr-1"></i> Pause</button>';
    if (mission.status==='PAUSED') h += '<button onclick="resumeMission(\''+mid+'\')" class="px-4 py-2 rounded-lg bg-green-600/20 text-green-400 hover:bg-green-600/30"><i class="fas fa-play mr-1"></i> Resume</button>';
    if (mission.status!=='COMPLETED' && mission.status!=='ABORTED') h += '<button onclick="abortMission(\''+mid+'\')" class="px-4 py-2 rounded-lg bg-red-600/20 text-red-400 hover:bg-red-600/30"><i class="fas fa-stop mr-1"></i> Abort</button>';
    h += '</div></div>';
    h += '<div class="grid grid-cols-5 gap-4">';
    h += '<div class="p-4 rounded-xl bg-white/5 text-center"><p class="text-2xl font-bold text-white">'+(mission.imagesAnalyzed||0)+'</p><p class="text-xs text-gray-500">Images</p></div>';
    h += '<div class="p-4 rounded-xl bg-white/5 text-center"><p class="text-2xl font-bold text-white">'+(mission.incidentsDetected||0)+'</p><p class="text-xs text-gray-500">Incidents</p></div>';
    h += '<div class="p-4 rounded-xl bg-white/5 text-center"><p class="text-2xl font-bold text-white">'+thoughts.length+'</p><p class="text-xs text-gray-500">Thoughts</p></div>';
    h += '<div class="p-4 rounded-xl bg-white/5 text-center"><p class="text-2xl font-bold text-white">'+(full.selfCorrections||mission.selfCorrections||0)+'</p><p class="text-xs text-gray-500">Self-Corrections</p></div>';
    h += '<div class="p-4 rounded-xl bg-white/5 text-center"><p class="text-2xl font-bold text-white">'+(mission.warningsIssued||0)+'</p><p class="text-xs text-gray-500">Warnings</p></div>';
    h += '</div>';
    // === Analyzed Images Gallery ===
    if (images && images.length > 0) {
        h += '<div><h4 class="text-lg font-semibold text-white mb-4 flex items-center gap-2"><i class="fas fa-images text-green-400"></i> Analyzed Images (' + images.length + ')</h4>';
        h += '<div class="grid grid-cols-1 md:grid-cols-2 gap-4">';
        images.forEach(function(img) {
            var riskColor = img.riskLevel==='CRITICAL'?'border-red-500 shadow-red-500/30':img.riskLevel==='HIGH'?'border-orange-500 shadow-orange-500/30':img.riskLevel==='MEDIUM'?'border-yellow-500 shadow-yellow-500/30':'border-green-500 shadow-green-500/30';
            var riskBg = img.riskLevel==='CRITICAL'?'bg-red-500':img.riskLevel==='HIGH'?'bg-orange-500':img.riskLevel==='MEDIUM'?'bg-yellow-500':'bg-green-500';
            var imgSrc = 'data:' + (img.mimeType||'image/jpeg') + ';base64,' + img.thumbnailBase64;
            h += '<div class="rounded-xl overflow-hidden border-2 ' + riskColor + ' shadow-lg transition-transform hover:scale-[1.02]">';
            h += '<div class="relative">';
            h += '<img src="' + imgSrc + '" alt="Analyzed image" class="w-full h-48 object-cover cursor-zoom-in" onclick="this.classList.toggle(\'h-48\');this.classList.toggle(\'h-auto\');this.classList.toggle(\'object-cover\');this.classList.toggle(\'object-contain\')" onerror="this.parentElement.innerHTML=\'<div class=\\\'w-full h-48 flex items-center justify-center bg-white/5 text-gray-500\\\'><i class=\\\'fas fa-image text-3xl\\\'></i><span class=\\\'ml-2 text-sm\\\'>Image unavailable</span></div>\'" />';
            h += '<span class="absolute top-2 right-2 px-2 py-1 rounded-md text-xs font-bold text-white ' + riskBg + '">' + (img.riskLevel||'?') + '</span>';
            h += '<span class="absolute bottom-2 left-2 px-2 py-1 rounded-md text-xs font-mono bg-black/60 text-white">' + (img.imageId||'') + '</span>';
            h += '</div>';
            h += '<div class="p-2 bg-white/5"><p class="text-xs text-gray-400 truncate"><i class="fas fa-map-pin mr-1"></i>' + (img.location||'Unknown') + '</p>';
            h += '<p class="text-xs text-gray-500 font-mono">' + (img.analyzedAt ? formatTime(img.analyzedAt) : '') + '</p></div>';
            h += '</div>';
        });
        h += '</div></div>';
    }
    if (full.correctionLog && full.correctionLog.length > 0) {
        h += '<div><h4 class="text-lg font-semibold text-white mb-3 flex items-center gap-2"><i class="fas fa-rotate text-pink-400"></i> Self-Correction Log (Gemini 3)</h4><div class="space-y-2">';
        full.correctionLog.forEach(function(log) { h += '<div class="p-3 rounded-lg bg-pink-500/10 border border-pink-500/20 text-sm"><code class="text-pink-300 font-mono text-xs">' + log + '</code></div>'; });
        h += '</div></div>';
    }
    h += '<div><h4 class="text-lg font-semibold text-white mb-4 flex items-center gap-2"><i class="fas fa-brain text-purple-400"></i> Thought Chain (' + thoughts.length + ' signatures)</h4><div class="space-y-3 max-h-72 overflow-y-auto">';
    if (thoughts.length > 0) {
        thoughts.slice(-20).reverse().forEach(function(t) {
            var lv = (t.thinkingLevel||'medium').toLowerCase();
            h += '<div class="thought-bubble thought-'+lv+' py-3 mb-2"><div class="flex items-center justify-between mb-2"><span class="text-xs font-mono text-gray-500"><i class="fas '+getThoughtLevelIcon(t.thinkingLevel)+'"></i> '+(t.thinkingLevel||'MEDIUM').toUpperCase()+'</span><span class="text-xs text-gray-600">'+(t.timestamp?formatTime(t.timestamp):'')+'</span></div><p class="text-sm text-gray-300">'+(t.reasoning||t.content||'')+'</p></div>';
        });
    } else { h += '<p class="text-gray-500 text-center py-4">No thoughts recorded yet</p>'; }
    h += '</div></div>';
    if (incidents.length > 0) {
        h += '<div><h4 class="text-lg font-semibold text-white mb-4 flex items-center gap-2"><i class="fas fa-triangle-exclamation text-red-400"></i> Incidents ('+incidents.length+')</h4><div class="space-y-3">';
        incidents.forEach(function(i) { h += renderIncident(i); });
        h += '</div></div>';
    }
    h += '</div>';
    return h;
}

function closeMissionDetailModal() { document.getElementById('missionDetailModal').classList.add('hidden'); }

async function pauseMission(id) { try { await fetch(API_V3+'/mission/'+id+'/pause', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({reason:'Manual pause'})}); showToast('Mission paused','warning'); closeMissionDetailModal(); await refreshMissions(); } catch(e) { showToast('Failed','error'); } }
async function resumeMission(id) { try { await fetch(API_V3+'/mission/'+id+'/resume', {method:'POST'}); showToast('Mission resumed','success'); closeMissionDetailModal(); await refreshMissions(); } catch(e) { showToast('Failed','error'); } }
async function abortMission(id) { if (!confirm('Abort this mission?')) return; try { await fetch(API_V3+'/mission/'+id, {method:'DELETE'}); showToast('Mission aborted','warning'); closeMissionDetailModal(); await refreshMissions(); } catch(e) { showToast('Failed','error'); } }

function addThought(t) { thoughts.unshift(t); if (thoughts.length > 50) thoughts.pop(); renderThoughtChain(); }

function renderThoughtChain() {
    var c = document.getElementById('thoughtChain');
    if (thoughts.length === 0) { c.innerHTML = '<div class="text-center py-12 text-gray-500"><i class="fas fa-lightbulb text-3xl mb-3 opacity-50"></i><p class="text-sm">Thoughts will appear as the agent reasons</p></div>'; return; }
    c.innerHTML = thoughts.map(function(t) {
        var lv = (t.level||t.thinkingLevel||'medium').toLowerCase();
        var lvU = lv.toUpperCase();
        var time = t.timestamp ? formatTime(t.timestamp) : 'now';
        return '<div class="thought-bubble thought-'+lv+' py-3 mb-3 animate-thought"><div class="flex items-center justify-between mb-2"><span class="text-xs font-mono text-gray-500"><i class="fas '+getThoughtLevelIcon(lvU)+'"></i> '+lvU+'</span><span class="text-xs text-gray-600">'+time+'</span></div><p class="text-sm text-gray-300">'+(t.content||t.reasoning||'')+'</p></div>';
    }).join('');
}

function getThoughtLevelIcon(l) { l = (l||'').toUpperCase(); return l==='LOW'?'fa-bolt text-blue-400':l==='HIGH'?'fa-fire text-pink-400':'fa-brain text-purple-400'; }

function renderIncident(i) {
    var rc = i.riskLevel==='CRITICAL'?'border-red-500':i.riskLevel==='HIGH'?'border-orange-500':i.riskLevel==='MEDIUM'?'border-yellow-500':i.riskLevel==='LOW'?'border-green-500':'border-gray-500';
    var h = '<div class="p-4 rounded-xl bg-white/5 border-l-4 '+rc+'"><div class="flex items-center justify-between mb-2"><div class="flex items-center gap-2"><span class="text-sm font-semibold text-white">'+(i.riskLevel||'UNKNOWN')+'</span><span class="text-xs text-gray-500">'+(i.incidentId||'')+'</span></div><span class="text-xs text-gray-500">'+formatTime(i.detectedAt)+'</span></div><p class="text-sm text-gray-400">'+(i.description||'')+'</p>';
    if (i.requiresEscalation) h += '<div class="mt-2 px-2 py-1 inline-flex items-center gap-1 text-xs bg-red-500/20 text-red-400 rounded"><i class="fas fa-exclamation-circle"></i> Requires Human Escalation</div>';
    h += '</div>';
    return h;
}

async function refreshStats() {
    try {
        var r = await fetch(API_V3 + '/stats');
        var s = await r.json();
        document.getElementById('activeMissionsCount').textContent = s.activeMissions || 0;
        document.getElementById('imagesAnalyzedCount').textContent = s.totalImagesAnalyzed || 0;
        document.getElementById('incidentsCount').textContent = s.totalIncidentsHandled || 0;
        document.getElementById('thoughtsCount').textContent = s.totalThoughtsGenerated || 0;
    } catch(e) {}
}

async function pollEvents() {
    try {
        var r = await fetch(API_V3 + '/events?clientId=' + CLIENT_ID);
        var d = await r.json();
        if (d.events && d.events.length > 0) {
            d.events.forEach(function(ev) {
                if (ev.type === 'INCIDENT') { incidents.unshift(ev); addThought({ level:'HIGH', content:'Incident detected: '+(ev.description||'Alert'), timestamp:ev.timestamp }); showToast('Incident: '+(ev.description||'').substring(0,60), 'warning'); renderIncidentsList(); }
                else if (ev.type === 'CHECKPOINT') { addThought({ level:'LOW', content:'Checkpoint: '+ev.imagesProcessed+' images, '+ev.incidentsDetected+' incidents', timestamp:ev.timestamp }); }
                else if (ev.type === 'ESCALATION') { showToast('Mission requires human attention!', 'error'); addThought({ level:'HIGH', content:'Escalation required for mission '+ev.missionId, timestamp:ev.timestamp }); }
            });
            refreshStats(); refreshMissions();
        }
    } catch(e) {}
}

function renderIncidentsList() {
    var c = document.getElementById('incidentsList');
    if (!c || incidents.length === 0) return;
    c.innerHTML = '<div class="space-y-3">'+incidents.slice(0,10).map(function(i){return renderIncident(i);}).join('')+'</div>';
}

function handleDragOver(e) { e.preventDefault(); e.currentTarget.classList.add('border-blue-500','bg-blue-500/10'); }
function handleDragLeave(e) { e.preventDefault(); e.currentTarget.classList.remove('border-blue-500','bg-blue-500/10'); }
function handleDrop(e) { e.preventDefault(); e.currentTarget.classList.remove('border-blue-500','bg-blue-500/10'); if (e.dataTransfer.files.length > 0) processFile(e.dataTransfer.files[0]); }
function handleFileSelect(e) { if (e.target.files.length > 0) processFile(e.target.files[0]); }

function processFile(file) {
    if (!file.type.startsWith('image/')) { showToast('Please select an image file', 'error'); return; }
    var reader = new FileReader();
    reader.onload = function(e) {
        currentImageData = { base64: e.target.result.split(',')[1], mimeType: file.type };
        document.getElementById('quickPreviewImage').src = e.target.result;
        document.getElementById('quickDropZone').classList.add('hidden');
        document.getElementById('quickPreview').classList.remove('hidden');
        updateAnalyzeButton();
    };
    reader.readAsDataURL(file);
}

function resetQuickAnalysis() {
    currentImageData = null; lastAnalysisResult = null;
    document.getElementById('quickDropZone').classList.remove('hidden');
    document.getElementById('quickPreview').classList.add('hidden');
    var res = document.getElementById('quickResult'); if (res) res.classList.add('hidden');
    var ph = document.getElementById('quickResultPlaceholder'); if (ph) ph.classList.remove('hidden');
    document.getElementById('quickFileInput').value = '';
}

function updateAnalyzeButton() {
    var btn = document.getElementById('btnQuickAnalyze');
    var sel = document.getElementById('missionSelect');
    btn.disabled = !currentImageData || !sel.value;
}

async function analyzeQuickImage() {
    var missionId = document.getElementById('missionSelect').value;
    if (!missionId || !currentImageData) return;
    var btn = document.getElementById('btnQuickAnalyze');
    btn.disabled = true; btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> Gemini 3 analyzing...';
    try {
        var r = await fetch(API_V3+'/mission/'+missionId+'/analyze', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({ image:currentImageData.base64, mimeType:currentImageData.mimeType }) });
        var result = await r.json();
        lastAnalysisResult = result;
        displayQuickResult(result);
        addThought({ level:result.thinkingLevel||'MEDIUM', content:'Image analyzed: Risk '+(result.riskLevel||'UNKNOWN')+' ('+(((result.riskScore||0)*100).toFixed(0))+'%) - '+(result.summary||'').substring(0,80), timestamp:new Date().toISOString() });
        showToast('Analysis complete - '+(result.riskLevel||'UNKNOWN'), 'success');
        await refreshMissions();
    } catch(e) { console.error('Analysis error:', e); showToast('Analysis failed: '+e.message, 'error'); }
    finally { btn.disabled = false; btn.innerHTML = '<i class="fas fa-wand-magic-sparkles mr-2"></i> Analyze with Gemini 3'; }
}

function displayQuickResult(result) {
    var placeholder = document.getElementById('quickResultPlaceholder');
    var container = document.getElementById('quickResult');
    if (placeholder) placeholder.classList.add('hidden');
    container.classList.remove('hidden');
    var riskScore = result.riskScore || 0;
    var badge = document.getElementById('riskBadge');
    badge.className = 'w-14 h-14 rounded-full flex items-center justify-center text-white font-bold text-lg shadow-lg ' + getRiskColor(result.riskLevel);
    badge.textContent = (riskScore * 100).toFixed(0);
    document.getElementById('quickRiskLevel').textContent = result.riskLevel || 'UNKNOWN';
    document.getElementById('quickRiskScore').textContent = 'Risk Score: ' + (riskScore * 100).toFixed(1) + '%';
    document.getElementById('quickSummaryText').textContent = result.summary || result.description || 'Analysis complete.';
    renderReasoningTab(result);
    renderCausalTab(result);
    renderActionsTab(result);
    showResultTab('reasoning');
}

function renderReasoningTab(result) {
    var c = document.getElementById('tabReasoning');
    var rp = result.reasoningProcess;
    if (!rp) { c.innerHTML = '<p class="text-gray-500 text-sm py-4">No reasoning data available</p>'; return; }
    var h = '';
    if (rp.visualObservations && rp.visualObservations.length) {
        h += '<h5 class="font-semibold text-white mb-2 flex items-center gap-2"><i class="fas fa-eye text-blue-400"></i> Visual Observations</h5><div class="space-y-2 mb-4">';
        rp.visualObservations.forEach(function(obs) { h += '<div class="p-2 rounded-lg bg-white/5 text-xs"><span class="font-semibold text-blue-300">'+(obs.element||'')+'</span><span class="text-gray-400 ml-1">('+(obs.spatialLocation||'')+')</span><p class="text-gray-300 mt-1">'+(obs.description||'')+'</p>'+(obs.confidence?'<span class="text-gray-600">Confidence: '+(obs.confidence*100).toFixed(0)+'%</span>':'')+'</div>'; });
        h += '</div>';
    }
    if (rp.inferenceChain && rp.inferenceChain.length) {
        h += '<h5 class="font-semibold text-white mb-2 flex items-center gap-2"><i class="fas fa-link text-purple-400"></i> Inference Chain</h5><ol class="list-decimal list-inside space-y-1 mb-4 text-gray-300">';
        rp.inferenceChain.forEach(function(step) { h += '<li class="text-xs">'+step+'</li>'; });
        h += '</ol>';
    }
    if (rp.riskJustification) { h += '<div class="p-2 rounded-lg bg-purple-500/10 border border-purple-500/20 text-xs text-purple-200 mb-3"><strong>Risk Justification:</strong> '+rp.riskJustification+'</div>'; }
    if (rp.uncertainties && rp.uncertainties.length) {
        h += '<h5 class="font-semibold text-yellow-300 mb-2 flex items-center gap-2"><i class="fas fa-question-circle text-yellow-400"></i> Uncertainties</h5><ul class="list-disc list-inside space-y-1 text-gray-400 text-xs">';
        rp.uncertainties.forEach(function(u) { h += '<li>'+u+'</li>'; });
        h += '</ul>';
    }
    c.innerHTML = h || '<p class="text-gray-500 text-sm py-4">No reasoning data</p>';
}

function renderCausalTab(result) {
    var c = document.getElementById('tabCausal');
    var ca = result.causalAnalysis;
    if (!ca) { c.innerHTML = '<p class="text-gray-500 text-sm py-4">No causal analysis available</p>'; return; }
    var h = '';
    if (ca.primaryCause) { h += '<div class="p-3 rounded-lg bg-red-500/10 border border-red-500/20 mb-4"><p class="text-xs font-semibold text-red-300 mb-1"><i class="fas fa-exclamation-triangle mr-1"></i> Primary Cause</p><p class="text-sm text-gray-200">'+ca.primaryCause+'</p></div>'; }
    if (ca.effectChains && ca.effectChains.length) {
        h += '<h5 class="font-semibold text-white mb-3 flex items-center gap-2"><i class="fas fa-diagram-project text-orange-400"></i> Effect Chains</h5>';
        ca.effectChains.forEach(function(chain) {
            h += '<div class="causal-chain mb-4">';
            h += '<div class="causal-node mb-3"><p class="text-xs text-orange-300 font-semibold">Cause</p><p class="text-xs text-gray-300">'+(chain.cause||'')+'</p></div>';
            h += '<div class="causal-node mb-3"><p class="text-xs text-yellow-300 font-semibold">Immediate Effect</p><p class="text-xs text-gray-300">'+(chain.immediateEffect||'')+'</p></div>';
            h += '<div class="causal-node mb-3"><p class="text-xs text-purple-300 font-semibold">Secondary Effect</p><p class="text-xs text-gray-300">'+(chain.secondaryEffect||'')+'</p></div>';
            h += '<div class="causal-node"><p class="text-xs text-red-300 font-semibold">Ecosystem Impact</p><p class="text-xs text-gray-300">'+(chain.ecosystemImpact||'')+'</p></div>';
            h += '</div>';
        });
    }
    if (ca.longTermConsequence) { h += '<div class="p-2 rounded-lg bg-white/5 text-xs text-gray-400 mt-3"><strong class="text-gray-300">Long-term:</strong> '+ca.longTermConsequence+'</div>'; }
    c.innerHTML = h || '<p class="text-gray-500 text-sm py-4">No causal analysis</p>';
}

function renderActionsTab(result) {
    var c = document.getElementById('tabActions');
    var h = '';
    if (result.recommendations && result.recommendations.length) {
        h += '<h5 class="font-semibold text-white mb-2 flex items-center gap-2"><i class="fas fa-bolt text-yellow-400"></i> Immediate Actions</h5><ul class="space-y-2 mb-4">';
        result.recommendations.forEach(function(a, i) { h += '<li class="flex items-start gap-2 text-xs"><span class="w-5 h-5 rounded-full bg-yellow-500/20 text-yellow-400 flex items-center justify-center flex-shrink-0 text-xs font-bold">'+(i+1)+'</span><span class="text-gray-300">'+a+'</span></li>'; });
        h += '</ul>';
    }
    if (result.guidelines && result.guidelines.length) {
        h += '<h5 class="font-semibold text-white mb-2 flex items-center gap-2"><i class="fas fa-book text-green-400"></i> Ethical Guidelines</h5><div class="space-y-2">';
        result.guidelines.forEach(function(g) { h += '<div class="p-2 rounded-lg bg-white/5 text-xs"><span class="px-1.5 py-0.5 rounded text-xs font-semibold '+getCategoryColor(g.category)+'">'+(g.category||'GENERAL')+'</span><p class="text-gray-300 mt-1">'+(g.guideline||'')+'</p>'+(g.culturalContext?'<p class="text-gray-500 mt-1 italic">'+g.culturalContext+'</p>':'')+'</div>'; });
        h += '</div>';
    }
    var ca = result.causalAnalysis;
    if (ca && ca.mitigationStrategies && ca.mitigationStrategies.length) {
        h += '<h5 class="font-semibold text-white mt-4 mb-2 flex items-center gap-2"><i class="fas fa-shield-halved text-eco-400"></i> Mitigation Strategies</h5><ul class="list-disc list-inside space-y-1 text-xs text-gray-400">';
        ca.mitigationStrategies.forEach(function(s) { h += '<li>'+s+'</li>'; });
        h += '</ul>';
    }
    c.innerHTML = h || '<p class="text-gray-500 text-sm py-4">No actions available</p>';
}

function getCategoryColor(cat) { cat=(cat||'').toUpperCase(); return cat==='WILDLIFE'?'bg-green-500/20 text-green-400':cat==='FLORA'?'bg-emerald-500/20 text-emerald-400':cat==='CULTURAL'?'bg-amber-500/20 text-amber-400':cat==='ENVIRONMENTAL'?'bg-blue-500/20 text-blue-400':cat==='SAFETY'?'bg-red-500/20 text-red-400':'bg-gray-500/20 text-gray-400'; }

function showResultTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(function(el) { el.classList.remove('active-tab'); });
    document.querySelectorAll('.tab-btn').forEach(function(el) { el.classList.remove('active'); });
    var tab = document.getElementById('tab' + tabName.charAt(0).toUpperCase() + tabName.slice(1));
    if (tab) tab.classList.add('active-tab');
    document.querySelectorAll('.tab-btn[data-tab="'+tabName+'"]').forEach(function(el) { el.classList.add('active'); });
}

function getRiskColor(level) { return level==='CRITICAL'?'bg-red-600':level==='HIGH'?'bg-orange-600':level==='MEDIUM'?'bg-yellow-600':level==='LOW'?'bg-green-600':'bg-gray-600'; }

function formatTime(iso) { if (!iso) return 'now'; try { return new Date(iso).toLocaleTimeString('en-US',{hour:'2-digit',minute:'2-digit',second:'2-digit'}); } catch(e) { return iso; } }

function showToast(message, type) {
    type = type || 'success';
    var toast = document.getElementById('toast');
    var icons = { success:'fa-check-circle text-green-400', error:'fa-times-circle text-red-400', warning:'fa-exclamation-triangle text-yellow-400', info:'fa-info-circle text-blue-400' };
    document.getElementById('toastIcon').className = 'fas ' + (icons[type]||icons.success);
    document.getElementById('toastMessage').textContent = message;
    toast.classList.remove('translate-y-20','opacity-0');
    toast.classList.add('translate-y-0','opacity-100');
    setTimeout(function() { toast.classList.add('translate-y-20','opacity-0'); toast.classList.remove('translate-y-0','opacity-100'); }, 4000);
}

document.addEventListener('change', function(e) { if (e.target.id === 'missionSelect') updateAnalyzeButton(); });

console.log('Marathon Agent Dashboard v3.0 ready - Gemini 3 Hackathon Edition');
