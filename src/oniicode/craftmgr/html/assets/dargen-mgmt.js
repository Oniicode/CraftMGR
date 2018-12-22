
Element.prototype.remove = function() {
    this.parentElement.removeChild(this);
}
NodeList.prototype.remove = HTMLCollection.prototype.remove = function() {
    for(var i = this.length - 1; i >= 0; i--) {
        if(this[i] && this[i].parentElement) {
            this[i].parentElement.removeChild(this[i]);
        } 
    }
}

function refresh_list() {
	
	
	var xhttp = new XMLHttpRequest();
	xhttp.open("GET", "?list=servers", true);
	xhttp.onload = function() {
		var response = this.responseText.split("\n");
		var set = "";
		if(response[0].indexOf('<html>') > -1){
			set = "{lang:error_pls_refresh}";
			window.location.reload(false); 
		}
		else
		for(var i = 0;i < response.length;i++){
			if(response[i]=="== REGISTERED SERVERS ==")
				continue;
			if(response[i]=="")
				continue;
			if(response[i]=="== END OF SERVERS LIST")
				break;
			
			var data = response[i].split("_");

			set += "<tr>";
			set += "<td>#"+data[0]+"</td>"; //ID
			set += "<td>"+data[1]+"</td>"; //PORT
			set += "<td><span class=\"badge badge-pill badge-light\">"+data[2]+"</span></td>" //STATUS
			set += "<td>"+data[3]+"</td>"; //DESC
			if (data[2]=="OFFLINE")
				set += `
				<td>
					<button onclick="server_start(`+data[0]+`)" class="btn btn-sm btn-outline-primary" style="margin-right: .5em;">
						<i class="fa fa-play" aria-hidden="true"></i> Start
					</button>
					<button onclick="server_delete(`+data[0]+`)" class="btn btn-sm btn-outline-secondary" style="margin-right: .5em;">
						<i class="fa fa-trash" aria-hidden="true"></i> Delete
					</button>
					<a href="?server=`+data[0]+`" class="btn btn-sm btn-outline-secondary" style="height: 31px; margin-top: 1px;"><i class="fa fa-terminal" aria-hidden="true"></i></a>
				</td>
				`;
			else
				set += `
				<td>
					<button onclick="server_stop(`+data[0]+`)" class="btn btn-sm btn-outline-primary" style="margin-right: .5em;">
						<i class="fa fa-stop" aria-hidden="true"></i> Stop
					</button>
					<button onclick="server_kill(`+data[0]+`)" class="btn btn-sm btn-outline-secondary" style="margin-right: .5em;">
						<i class="fa fa-bolt" aria-hidden="true"></i> Force Stop
					</button>
					<a href="?server=`+data[0]+`" class="btn btn-sm btn-outline-secondary" style="height: 31px; margin-top: 1px;"><i class="fa fa-terminal" aria-hidden="true"></i></a>
				</td>`;
			set += "</tr>";
		}
		document.getElementById("serverlist").innerHTML = set;
	};
	xhttp.send();
	
}

function serverview_refresh(id){
	var xhttp = new XMLHttpRequest();
	xhttp.open("GET", "?status="+id, true);
	xhttp.onload = function() {
		var response = this.responseText + "";
		if(response.indexOf('<html>') > -1){
			document.getElementById("console").innerHTML = {lang:error_pls_refresh};
			window.location.reload(false); 
		}
		
		document.getElementById("sstatus").innerHTML = response;
		
		
		if(response == "OFFLINE"){
			document.getElementById("btns").innerHTML = `
				<a href="/" class="btn btn-light"><i class="fa fa-arrow-left" aria-hidden="true"></i></a>
				<button onclick="server_start(`+id+`)" class="btn btn-sm btn-primary"><i class="fa fa-play" aria-hidden="true"></i> Start</button>
			`;
		}else{
			document.getElementById("btns").innerHTML = `
				<a href="/" class="btn btn-light"><i class="fa fa-arrow-left" aria-hidden="true"></i></a>
				<button onclick="server_stop(`+id+`)" class="btn btn-sm btn-primary"><i class="fa fa-stop" aria-hidden="true"></i> Stop</button>
				<button onclick="server_kill(`+id+`)" class="btn btn-sm btn-secondary"><i class="fa fa-bolt" aria-hidden="true"></i> Force Stop</button>
			`;
		}
	};
	xhttp.send();
	
	
}

function refresh_console(id){
	var xhttp = new XMLHttpRequest();
	xhttp.open("GET", "?console="+id, true);
	xhttp.onload = function() {
		var response = this.responseText.replace(/(?:\r\n|\r|\n)/g, '<br />');
		if(document.getElementById("console").innerHTML != response){
			document.getElementById("console").innerHTML = response;
			document.getElementById("console").scrollTop = document.getElementById("console").scrollHeight;
		}
	};
	xhttp.send();
	
}

function server_command(id, cmd){
	setTimeout(function() {
		var xhttp = new XMLHttpRequest();
		xhttp.open("GET", "?cmd="+id+"&com="+cmd, false);
		xhttp.send();
		var resp = xhttp.responseText;
		if(resp!="cmd "+id+" "+cmd+"\nSUCCESS"){
			$.notify({lang:server_cmd_fail}, 'error', {
				  style: 'bootstrap'
				});
			refresh_list();
		}else{
			refresh_console(id);
		}
    }, 0);
	
	
}

function server_start(id) {
	$.notify({lang:server_start}, 'info', {
		  style: 'bootstrap'
		});
	var xhttp = new XMLHttpRequest();
	xhttp.open("GET", "?start="+id, false);
	xhttp.send();
	var resp = xhttp.responseText;
	if(resp=="start "+id+"\nSUCCESS"){
		$.notify({lang:server_start_success}, 'success', {
			  style: 'bootstrap'
			});
		refresh_list();
	}else{
		$.notify({lang:server_start_fail}, 'error', {
			  style: 'bootstrap'
			});
		refresh_list();
	}
}

function server_stop(id) {
	var xhttp = new XMLHttpRequest();
	xhttp.open("GET", "?stop="+id, false);
	xhttp.send();
	var resp = xhttp.responseText;
	if(resp=="stop "+id+"\nSUCCESS"){
		$.notify({lang:server_stop}, 'success', {
			  style: 'bootstrap'
			});
		refresh_list();
	}else{
		$.notify({lang:server_stop_fail}, 'error', {
			  style: 'bootstrap'
			});
		refresh_list();
	}
}

function server_kill(id) {
	var xhttp = new XMLHttpRequest();
	xhttp.open("GET", "?kill="+id, false);
	xhttp.send();
	var resp = xhttp.responseText;
	if(resp=="kill "+id+"\nSUCCESS"){
		$.notify({lang:server_kill_success}, 'success', {
			  style: 'bootstrap'
			});
		refresh_list();
	}else{
		$.notify({lang:server_kill_fail}, 'error', {
			  style: 'bootstrap'
			});
		refresh_list();
	}
}

function server_delete(id) {
	var xhttp = new XMLHttpRequest();
	xhttp.open("GET", "?delete="+id, false);
	xhttp.send();
	var resp = xhttp.responseText;
	if(resp=="delete "+id+"\nSUCCESS"){
		$.notify({lang:server_del_sucess}, 'success', {
			  style: 'bootstrap'
			});
		refresh_list();
	}else{
		$.notify({lang:server_del_fail}, 'error', {
			  style: 'bootstrap'
			});
		refresh_list();
	}
}

function server_create(port, name, ram, template, autostart) {
	if(template.endsWith(".zip")){
		$.notify({lang:server_create}, 'info', {
			  style: 'bootstrap'
			});
	}
	var xhttp = new XMLHttpRequest();
	xhttp.open("GET", "?create="+port+"&template="+template+"&desc="+name+"&memory="+ram+"&autostart="+autostart, true);
	xhttp.onload = function() {
		var resp = this.responseText;
		if(resp.startsWith("create SUCCESS")){
			refresh_list();
			$.notify({lang:server_create_success}, 'success', { 
				  style: 'bootstrap'
				}); 
		}else{
			refresh_list();
			$.notify('[Debug]: '+resp, 'info', {
				  style: 'bootstrap'
				});
		}
	}; 
	xhttp.send();
}

function server_backup_apply(id, desc){
	var xhttp = new XMLHttpRequest();
	xhttp.open("GET", "?backup-apply="+id+"&desc="+desc, true);
	xhttp.onload = function() {
		var resp = this.responseText;
		if(resp=="backup-apply "+id+"\nSUCCESS"){
			$.notify({lang:server_backup_apply_success}, 'success', {
				  style: 'bootstrap'
				});
			window.location.reload(false); 
		}else{
			$.notify({lang:server_backup_apply_fail}, 'error', {
				  style: 'bootstrap'
				});
		}
	}
	xhttp.send();
}

function server_backup_delete(id, desc){
	var xhttp = new XMLHttpRequest();
	xhttp.open("GET", "?backup-delete="+id+"&desc="+desc, true);
	xhttp.onload = function() {
		var resp = this.responseText;
		if(resp=="backup-delete "+id+"\nSUCCESS"){
			$.notify({lang:server_backup_delete_success}, 'success', {
				  style: 'bootstrap'
				});
			window.location.reload(false); 
		}else{
			$.notify({lang:server_backup_delete_fail}, 'error', {
				  style: 'bootstrap'
				});
		}
	}
	xhttp.send();
}

function server_mkbackup(id, desc){
	$.notify({lang:server_mkbackup}, 'info', {
		  style: 'bootstrap'
		});
	
	var xhttp = new XMLHttpRequest();
	xhttp.open("GET", "?backup-create="+id+"&desc="+desc, true);
	xhttp.onload = function() {
		var resp = this.responseText;
		if(resp=="backup-create "+id+"\nSUCCESS"){
			$.notify({lang:server_mkbackup_success}, 'success', {
				  style: 'bootstrap'
				});
			window.location.reload(false); 
		}else{
			$.notify({lang:server_mkbackup_fail}, 'error', {
				  style: 'bootstrap'
				});
		}
	}
	xhttp.send();
	
	
}