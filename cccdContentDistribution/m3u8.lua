require("os")
require("ngx")

function string.endswith(s, suffix)
	return s:sub(#s - #suffix + 1) == suffix
end

local htmlDir = '/usr/local/nginx/html'

local res = ""
local uri_w = string.sub(ngx.var.uri, 0,string.find(ngx.var.uri, "/[^/]*$") )
for line in io.lines(htmlDir .. ngx.var.uri) do
	if string.endswith(line,".ts") or string.endswith(line,".m3u8") then
		res = res .. line .. "?st=" .. ngx.encode_base64(ngx.md5_bin(ngx.var.secure_word..uri_w..line..ngx.var.arg_e)):gsub("=",""):gsub("+","-"):gsub("/","_") .. "&e=" .. ngx.var.arg_e .. "\n"
	else
		res = res .. line .. "\n"
	end
end
ngx.say(res)
