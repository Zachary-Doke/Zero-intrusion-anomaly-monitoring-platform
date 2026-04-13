#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

function refName(schema) {
  if (!schema || !schema.$ref) {
    return null;
  }
  const segments = schema.$ref.split('/');
  return segments[segments.length - 1] || null;
}

function schemaLabel(schema) {
  if (!schema) {
    return '未知';
  }
  const name = refName(schema);
  if (name) {
    return name;
  }
  if (schema.type === 'array' && schema.items) {
    return `array<${schemaLabel(schema.items)}>`;
  }
  if (schema.type) {
    return schema.type;
  }
  return '对象';
}

function getOperations(spec) {
  const allowed = new Set(['get', 'post', 'put', 'patch', 'delete']);
  const operations = [];
  for (const [route, routeItem] of Object.entries(spec.paths || {})) {
    const routeParams = Array.isArray(routeItem.parameters) ? routeItem.parameters : [];
    for (const [method, op] of Object.entries(routeItem)) {
      if (!allowed.has(method)) {
        continue;
      }
      const opParams = Array.isArray(op.parameters) ? op.parameters : [];
      const mergedParams = [...routeParams, ...opParams];
      operations.push({
        route,
        method: method.toUpperCase(),
        op,
        parameters: mergedParams
      });
    }
  }
  operations.sort((a, b) => {
    const routeCmp = a.route.localeCompare(b.route);
    if (routeCmp !== 0) {
      return routeCmp;
    }
    return a.method.localeCompare(b.method);
  });
  return operations;
}

function buildMarkdown(spec, sourcePath) {
  const lines = [];
  const title = (spec.info && spec.info.title) || 'API 文档';
  const version = (spec.info && spec.info.version) || 'unknown';
  const description = (spec.info && spec.info.description) || '';
  const generatedAt = new Date().toISOString();

  lines.push(`# ${title}`);
  lines.push('');
  lines.push(`- 版本: \`${version}\``);
  lines.push(`- 生成时间(UTC): \`${generatedAt}\``);
  lines.push(`- 来源: \`${sourcePath}\``);
  lines.push(`- 生成命令: \`bash scripts/export_openapi.sh\``);
  lines.push('');
  if (description) {
    lines.push(description);
    lines.push('');
  }

  const operations = getOperations(spec);
  lines.push('## 接口目录');
  lines.push('');
  for (const item of operations) {
    lines.push(`- ${item.method} ${item.route}`);
  }
  lines.push('');

  const groups = new Map();
  for (const item of operations) {
    const tags = Array.isArray(item.op.tags) && item.op.tags.length > 0 ? item.op.tags : ['Untagged'];
    const tag = tags[0];
    if (!groups.has(tag)) {
      groups.set(tag, []);
    }
    groups.get(tag).push(item);
  }

  for (const [tag, items] of groups.entries()) {
    lines.push(`## ${tag}`);
    lines.push('');

    for (const item of items) {
      lines.push(`### ${item.method} ${item.route}`);
      lines.push('');
      lines.push(`- 摘要: ${item.op.summary || '未提供'}`);
      lines.push(`- operationId: \`${item.op.operationId || 'N/A'}\``);

      if (item.parameters.length > 0) {
        const labels = item.parameters.map((p) => {
          const type = p.schema ? schemaLabel(p.schema) : '未知';
          const required = p.required ? 'required' : 'optional';
          return `\`${p.in}.${p.name}\` (${type}, ${required})`;
        });
        lines.push(`- 参数: ${labels.join('；')}`);
      } else {
        lines.push('- 参数: 无');
      }

      const requestJson = item.op.requestBody
        && item.op.requestBody.content
        && item.op.requestBody.content['application/json']
        && item.op.requestBody.content['application/json'].schema;
      if (requestJson) {
        lines.push(`- 请求体: \`application/json\` (${schemaLabel(requestJson)})`);
      } else {
        lines.push('- 请求体: 无');
      }

      const responses = item.op.responses || {};
      const responseLabels = Object.entries(responses).map(([code, info]) => {
        const schema = info.content
          && info.content['application/json']
          && info.content['application/json'].schema;
        if (schema) {
          return `\`${code}\` (${schemaLabel(schema)})`;
        }
        return `\`${code}\``;
      });
      lines.push(`- 响应: ${responseLabels.length > 0 ? responseLabels.join('；') : '未定义'}`);
      lines.push('');
    }
  }

  return `${lines.join('\n')}\n`;
}

function main() {
  const root = path.resolve(__dirname, '..');
  const input = process.argv[2] || path.join(root, 'docs', 'api', 'openapi.json');
  const output = process.argv[3] || path.join(root, 'docs', 'api', 'API.md');

  if (!fs.existsSync(input)) {
    throw new Error(`OpenAPI 文件不存在: ${input}`);
  }

  const raw = fs.readFileSync(input, 'utf8');
  const spec = JSON.parse(raw);
  const markdown = buildMarkdown(spec, path.relative(root, input));

  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(output, markdown, 'utf8');
  console.log(`[docs] API markdown generated: ${output}`);
}

main();
